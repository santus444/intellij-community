/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.ui.components.fields.CommaSeparatedIntegersField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.fields.valueEditors.CommaSeparatedIntegersValueEditor;
import com.intellij.ui.components.fields.valueEditors.ValueEditor;
import com.intellij.ui.components.labels.ActionLink;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Can be used for languages which do not use standard "Wrapping and Braces" panel.
 * <p>
 * <strong>Note</strong>: besides adding the panel to UI it is necessary to make sure that language's own
 * {@code LanguageCodeStyleSettingsProvider} explicitly supports RIGHT_MARGIN field in {@code customizeSettings()}
 * method as shown below:
 * <pre>
 * public void customizeSettings(...) {
 *   if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
 *     consumer.showStandardOptions("RIGHT_MARGIN");
 *   }
 * }
 * </pre>
 * @author Rustam Vishnyakov
 */
public class RightMarginForm {
  private IntegerField myRightMarginField;
  private JPanel myTopPanel;
  private JComboBox myWrapOnTypingCombo;
  private CommaSeparatedIntegersField myVisualGuidesField;
  @SuppressWarnings("unused") private ActionLink myResetLink;
  private final Language myLanguage;
  private final CodeStyleSettings mySettings;

  public RightMarginForm(@NotNull Language language, @NotNull CodeStyleSettings settings) {
    myLanguage = language;
    mySettings = settings;

    //noinspection unchecked
    myWrapOnTypingCombo.setModel(new DefaultComboBoxModel(
      CodeStyleSettingsCustomizable.WRAP_ON_TYPING_OPTIONS
    ));
  }

  void createUIComponents() {
    myRightMarginField = new IntegerField(ApplicationBundle.message("editbox.right.margin.columns"), 0, CodeStyleSettings.MAX_RIGHT_MARGIN);
    myRightMarginField.getValueEditor().addListener(new ValueEditor.Listener<Integer>() {
      @Override
      public void valueChanged(@NotNull Integer newValue) {
        myResetLink.setVisible(!newValue.equals(myRightMarginField.getDefaultValue()));
        myRightMarginField.getEmptyText().setText(getDefaultRightMarginText(mySettings));
      }
    });
    myRightMarginField.setCanBeEmpty(true);
    myRightMarginField.setDefaultValue(-1);
    myVisualGuidesField = new CommaSeparatedIntegersField(ApplicationBundle.message("settings.code.style.visual.guides"), 0, CodeStyleSettings.MAX_RIGHT_MARGIN, "Optional");
    myVisualGuidesField.getValueEditor().addListener(new ValueEditor.Listener<List<Integer>>() {
      @Override
      public void valueChanged(@NotNull List<Integer> newValue) {
        myVisualGuidesField.getEmptyText().setText(getDefaultVisualGuidesText(mySettings));
      }
    });
    myResetLink = new ActionLink("Reset", new ResetRightMarginAction());
  }

  private class ResetRightMarginAction extends DumbAwareAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
      myRightMarginField.resetToDefault();
    }
  }

  public void reset(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    myRightMarginField.setValue(langSettings.RIGHT_MARGIN);
    for (int i = 0; i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length; i ++) {
      if (langSettings.WRAP_ON_TYPING == CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i]) {
        myWrapOnTypingCombo.setSelectedIndex(i);
        break;
      }
    }
    myVisualGuidesField.setValue(langSettings.getSoftMargins());
    myResetLink.setVisible(langSettings.RIGHT_MARGIN >= 0);
    myRightMarginField.getEmptyText().setText(getDefaultRightMarginText(settings));
    myVisualGuidesField.getEmptyText().setText(getDefaultVisualGuidesText(settings));
  }

  public void apply(@NotNull CodeStyleSettings settings) throws ConfigurationException {
    myRightMarginField.validateContent();
    myVisualGuidesField.validateContent();
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    if (langSettings != settings) {
      langSettings.RIGHT_MARGIN = myRightMarginField.getValue();
    }
    langSettings.WRAP_ON_TYPING = getSelectedWrapOnTypingValue();
    settings.setSoftMargins(myLanguage, myVisualGuidesField.getValue());
  }

  public boolean isModified(@NotNull CodeStyleSettings settings) {
    CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
    return langSettings.RIGHT_MARGIN != myRightMarginField.getValue() ||
           langSettings.WRAP_ON_TYPING != getSelectedWrapOnTypingValue() ||
           !langSettings.getSoftMargins().equals(myVisualGuidesField.getValue());
  }


  private int getSelectedWrapOnTypingValue() {
    int i = myWrapOnTypingCombo.getSelectedIndex();
    if (i >= 0 && i < CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES.length) {
      return CodeStyleSettingsCustomizable.WRAP_ON_TYPING_VALUES[i];
    }
    return CommonCodeStyleSettings.WrapOnTyping.DEFAULT.intValue;
  }

  public JPanel getTopPanel() {
    return myTopPanel;
  }

  private static String getDefaultRightMarginText(@NotNull CodeStyleSettings settings) {
    return "Default: " + settings.getDefaultRightMargin();
  }

  private static String getDefaultVisualGuidesText(@NotNull CodeStyleSettings settings) {
    List<Integer> softMargins = settings.getSoftMargins();
    return "Default: " +
           (softMargins.size() > 0 ? CommaSeparatedIntegersValueEditor.intListToString(settings.getSoftMargins()) : "None");
  }
}
