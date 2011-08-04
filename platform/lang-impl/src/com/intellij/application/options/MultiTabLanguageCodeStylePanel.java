/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.application.options;

import com.intellij.application.options.codeStyle.*;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rustam Vishnyakov
 */

public abstract class MultiTabLanguageCodeStylePanel extends CodeStyleAbstractPanel {

  private CodeStyleAbstractPanel myActiveTab;
  private List<CodeStyleAbstractPanel> myTabs;
  private JPanel myPanel;
  private JTabbedPane myTabbedPane;

  protected MultiTabLanguageCodeStylePanel(@Nullable Language language, CodeStyleSettings currentSettings, CodeStyleSettings settings) {
    super(language, currentSettings, settings);
  }

  protected void initTabs(CodeStyleSettings settings) {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
    if (provider != null && !provider.usesSharedPreview()) {
      addTab(new MySpacesPanel(settings));
      addTab(new MyBlankLinesPanel(settings));
      addTab(new MyWrappingAndBracesPanel(settings));
    }
  }

  private void ensureTabs() {
    if (myTabs == null) {
      myPanel = new JPanel();
      myPanel.setLayout(new BorderLayout());
      myTabbedPane = new JTabbedPane();
      myTabs = new ArrayList<CodeStyleAbstractPanel>();
      myPanel.add(myTabbedPane);
      initTabs(getSettings());
    }
    assert !myTabs.isEmpty();
  }

  protected final void addTab(CodeStyleAbstractPanel tab) {
    myTabs.add(tab);
    tab.setShouldUpdatePreview(true);
    addPanelToWatch(tab.getPanel());
    myTabbedPane.addTab(tab.getTabTitle(), tab.getPanel());
    if (myActiveTab == null) {
      myActiveTab = tab;
    }
  }

  private void addTab(Configurable configurable) {
    ConfigurableWrapper wrapper = new ConfigurableWrapper(configurable, getSettings());
    addTab(wrapper);
  }

  protected final void createTab(CodeStyleSettingsProvider provider) {
    if (provider.hasSettingsPage()) return;
    Configurable configurable = provider.createSettingsPage(getCurrentSettings(), getSettings());
    addTab(configurable);
  }

  @Override
  public final void setModel(CodeStyleSchemesModel model) {
    super.setModel(model);
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setModel(model);
    }
  }

  @Override
  protected int getRightMargin() {
    ensureTabs();
    return myActiveTab.getRightMargin();
  }

  @Override
  protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
    ensureTabs();
    return myActiveTab.createHighlighter(scheme);
  }

  @NotNull
  @Override
  protected FileType getFileType() {
    ensureTabs();
    return myActiveTab.getFileType();
  }

  @Override
  protected String getPreviewText() {
    ensureTabs();
    return myActiveTab.getPreviewText();
  }

  @Override
  protected void updatePreview(boolean useDefaultSample) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.updatePreview(useDefaultSample);
    }
  }

  @Override
  public void onSomethingChanged() {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.setShouldUpdatePreview(true);
      tab.onSomethingChanged();
    }
  }

  @Override
  protected void somethingChanged() {
    super.somethingChanged();
  }

  @Override
  public void apply(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.apply(settings);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    for (CodeStyleAbstractPanel tab : myTabs) {
      Disposer.dispose(tab);
    }
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      if (tab.isModified(settings)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(CodeStyleSettings settings) {
    ensureTabs();
    for (CodeStyleAbstractPanel tab : myTabs) {
      tab.resetImpl(settings);
    }
  }

  private class MySpacesPanel extends CodeStyleSpacesPanel {

    public MySpacesPanel(CodeStyleSettings settings) {
      super(settings);
      setPanelLanguage(MultiTabLanguageCodeStylePanel.this.getDefaultLanguage());
    }

    @Override
    protected void installPreviewPanel(JPanel previewPanel) {
      previewPanel.setLayout(new BorderLayout());
      previewPanel.add(getEditor().getComponent(), BorderLayout.CENTER);
    }

    @Override
    protected void customizeSettings() {
      customizePanel(this);
    }

    @Override
    protected boolean shouldHideOptions() {
      return true;
    }
  }

  private class MyBlankLinesPanel extends CodeStyleBlankLinesPanel {

    public MyBlankLinesPanel(CodeStyleSettings settings) {
      super(settings);
      setPanelLanguage(MultiTabLanguageCodeStylePanel.this.getDefaultLanguage());
    }

    @Override
    protected void customizeSettings() {
      customizePanel(this);
    }

    @Override
    protected void installPreviewPanel(JPanel previewPanel) {
      previewPanel.setLayout(new BorderLayout());
      previewPanel.add(getEditor().getComponent(), BorderLayout.CENTER);
    }

  }

  private class MyWrappingAndBracesPanel extends WrappingAndBracesPanel {

    public MyWrappingAndBracesPanel(CodeStyleSettings settings) {
      super(settings);
      setPanelLanguage(MultiTabLanguageCodeStylePanel.this.getDefaultLanguage());
    }

    @Override
    protected void customizeSettings() {
      customizePanel(this);
    }

    @Override
    protected void installPreviewPanel(JPanel previewPanel) {
      previewPanel.setLayout(new BorderLayout());
      previewPanel.add(getEditor().getComponent(), BorderLayout.CENTER);
    }
  }

  private void customizePanel(MultilanguageCodeStyleAbstractPanel panel) {
    LanguageCodeStyleSettingsProvider provider = LanguageCodeStyleSettingsProvider.forLanguage(getDefaultLanguage());
    if (provider != null) {
      provider.customizeSettings(panel, panel.getSettingsType());
    }
  }


  //========================================================================================================================================

  private static class ConfigurableWrapper extends CodeStyleAbstractPanel {

    private Configurable myConfigurable;

    public ConfigurableWrapper(@NotNull Configurable configurable, CodeStyleSettings settings) {
      super(settings);
      myConfigurable = configurable;
    }

    @Override
    protected int getRightMargin() {
      return 0;
    }

    @Nullable
    @Override
    protected EditorHighlighter createHighlighter(EditorColorsScheme scheme) {
      return null;
    }

    @SuppressWarnings("ConstantConditions")
    @NotNull
    @Override
    protected FileType getFileType() {
      Language language = getDefaultLanguage();
      return language != null ? language.getAssociatedFileType() : FileTypes.PLAIN_TEXT;
    }

    @Override
    protected String getTabTitle() {
      return myConfigurable.getDisplayName();
    }

    @Override
    protected String getPreviewText() {
      return null;
    }

    @Override
    public void apply(CodeStyleSettings settings) {
      try {
        myConfigurable.apply();
      }
      catch (ConfigurationException e) {
        // Ignore
      }
    }

    @Override
    public boolean isModified(CodeStyleSettings settings) {
      return myConfigurable.isModified();
    }

    @Nullable
    @Override
    public JComponent getPanel() {
      return myConfigurable.createComponent();
    }

    @Override
    protected void resetImpl(CodeStyleSettings settings) {
      myConfigurable.reset();
    }
  }
}