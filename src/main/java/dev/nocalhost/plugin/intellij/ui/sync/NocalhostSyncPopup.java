package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.CustomIconProvider;
import com.intellij.dvcs.ui.PopupElementWithAdditionalInfo;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.ui.FlatSpeedSearchPopup;
import com.intellij.openapi.vcs.ui.PopupListElementRendererWithIcon;
import com.intellij.ui.ErrorLabel;
import com.intellij.ui.GroupHeaderSeparator;
import com.intellij.ui.JBColor;
import com.intellij.ui.RelativeFont;
import com.intellij.ui.SeparatorWithText;
import com.intellij.ui.components.panels.OpaquePanel;
import com.intellij.ui.popup.WizardPopup;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.FontUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import javax.swing.*;

import static com.intellij.util.ui.UIUtil.DEFAULT_HGAP;
import static com.intellij.util.ui.UIUtil.DEFAULT_VGAP;

public class NocalhostSyncPopup extends FlatSpeedSearchPopup {
    private MyPopupListElementRenderer myListElementRenderer;

    public NocalhostSyncPopup(@Nullable @NlsContexts.PopupTitle String title,
                                @NotNull ActionGroup actionGroup,
                                @NotNull DataContext dataContext) {
        super(title, actionGroup, dataContext, null, false);
        setSpeedSearchAlwaysShown();
    }

    //for child popups only
    private NocalhostSyncPopup(@Nullable WizardPopup aParent, @NotNull ListPopupStep aStep, @Nullable Object parentValue) {
        super(aParent, aStep, DataContext.EMPTY_CONTEXT, parentValue);
    }

    @Override
    protected WizardPopup createPopup(WizardPopup parent, PopupStep step, Object parentValue) {
        return createListPopupStep(parent, step, parentValue);
    }

    private WizardPopup createListPopupStep(WizardPopup parent, PopupStep step, Object parentValue) {
        if (step instanceof ListPopupStep) {
            return new NocalhostSyncPopup(parent, (ListPopupStep)step, parentValue);
        }
        return super.createPopup(parent, step, parentValue);
    }

    @Override
    protected MyPopupListElementRenderer getListElementRenderer() {
        if (myListElementRenderer == null) {
            myListElementRenderer = new MyPopupListElementRenderer(this);
        }
        return myListElementRenderer;
    }

    private static class MyPopupListElementRenderer extends PopupListElementRendererWithIcon {
        private ErrorLabel myInfoLabel;

        MyPopupListElementRenderer(ListPopupImpl aPopup) {
            super(aPopup);
        }

        @Override
        protected SeparatorWithText createSeparator() {
            SeparatorWithText separator = super.createSeparator();
            separator.setTextForeground(UIUtil.getListForeground());
            separator.setCaptionCentered(false);
            UIUtil.addInsets(separator, DEFAULT_VGAP, UIUtil.getListCellHPadding(), 0, 0);
            return separator;
        }

        @Override
        protected void setSeparatorFont(Font font) {
            super.setSeparatorFont(RelativeFont.BOLD.derive(font));
        }

        @Override
        protected void customizeComponent(JList list, Object value, boolean isSelected) {
            super.customizeComponent(list, value, isSelected);
            if (mySeparatorComponent.isVisible()) {
                ((GroupHeaderSeparator)mySeparatorComponent).setHideLine(myCurrentIndex == 0 || StringUtil.isNotEmpty(mySeparatorComponent.getCaption()));
            }

            CustomIconProvider actionWithIconProvider = getSpecificAction(value, CustomIconProvider.class);
            if (actionWithIconProvider != null) {
                myTextLabel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                myTextLabel.setIcon(actionWithIconProvider.getRightIcon());
            }
            PopupElementWithAdditionalInfo additionalInfoAction = getSpecificAction(value, PopupElementWithAdditionalInfo.class);
            updateInfoComponent(myInfoLabel, additionalInfoAction != null ? additionalInfoAction.getInfoText() : null, isSelected);
        }

        private void updateInfoComponent(@NotNull ErrorLabel infoLabel, @Nullable @Nls String infoText, boolean isSelected) {
            if (infoText != null) {
                infoLabel.setVisible(true);
                infoLabel.setText(infoText);

                if (isSelected) {
                    setSelected(infoLabel);
                }
                else {
                    infoLabel.setBackground(getBackground());
                    infoLabel.setForeground(JBColor.GRAY);
                }
            }
            else {
                infoLabel.setVisible(false);
            }
        }

        @Override
        protected JComponent createItemComponent() {
            myTextLabel = new ErrorLabel();
            myTextLabel.setOpaque(true);
            myTextLabel.setBorder(JBUI.Borders.empty(1));

            myInfoLabel = new ErrorLabel();
            myInfoLabel.setOpaque(true);
            myInfoLabel.setBorder(JBUI.Borders.empty(1, DEFAULT_HGAP, 1, 1));
            myInfoLabel.setFont(FontUtil.minusOne(myInfoLabel.getFont()));

            JPanel compoundPanel = new OpaquePanel(new BorderLayout(), JBColor.WHITE);
            myIconLabel = new IconComponent();
            myInfoLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            JPanel compoundTextPanel = new OpaquePanel(new BorderLayout(), compoundPanel.getBackground());
            JPanel textPanel = new OpaquePanel(new BorderLayout(), compoundPanel.getBackground());
            compoundPanel.add(myIconLabel, BorderLayout.WEST);
            textPanel.add(myTextLabel, BorderLayout.WEST);
            textPanel.add(myInfoLabel, BorderLayout.CENTER);
            compoundTextPanel.add(textPanel, BorderLayout.CENTER);
            compoundPanel.add(compoundTextPanel, BorderLayout.CENTER);
            return layoutComponent(compoundPanel);
        }
    }
}
