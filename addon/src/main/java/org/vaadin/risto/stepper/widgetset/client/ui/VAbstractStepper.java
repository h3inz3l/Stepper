package org.vaadin.risto.stepper.widgetset.client.ui;

import org.vaadin.risto.stepper.widgetset.client.ui.helpers.UpDownControls;
import org.vaadin.risto.stepper.widgetset.client.ui.helpers.UpDownTextBox;
import org.vaadin.risto.stepper.widgetset.client.ui.helpers.ValueUpdateTimer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * 
 * @author Risto Yrjänä / Vaadin }>
 * 
 */
public abstract class VAbstractStepper<T, S> extends FlowPanel implements
        ValueChangeHandler<String> {

    public static final String CLASSNAME = "v-stepper";

    public static final int valueRepeatDelay = 150;

    protected final UpDownTextBox textBox;

    protected final UpDownControls upDownControls;

    protected final int updateDelay = 300;

    protected final ValueUpdateTimer valueUpdateTimer;

    protected boolean timerHasChangedValue;

    private boolean isDisabled;

    private boolean isReadonly;

    private boolean isManualInputAllowed;

    private boolean mouseWheelEnabled;

    private boolean invalidValuesAllowed;

    private S stepAmount;
    private T maxValue;
    private T minValue;

    private String value;

    private RegExp valueRegexp;

    private boolean isValueFilteringEnabled;

    public VAbstractStepper() {
        this(".*"); // match all by default
    }

    public VAbstractStepper(String valueRegexp) {

        setStyleName(CLASSNAME);

        textBox = new UpDownTextBox(this);
        this.valueRegexp = RegExp.compile(valueRegexp);
        upDownControls = new UpDownControls(this);

        add(textBox);
        add(upDownControls);

        valueUpdateTimer = new ValueUpdateTimer(this);

        textBox.addValueChangeHandler(this);
    }

    /**
     * Calculate the string value of <code>current value + 1</code>
     * 
     * @param startValue
     * @return
     * @throws Exception
     */
    protected abstract String getIncreasedValue(String startValue)
            throws Exception;

    /**
     * Calculate the string value of <code>current value - 1</code>
     * 
     * @param startValue
     * @return
     * @throws Exception
     */
    protected abstract String getDecreasedValue(String startValue)
            throws Exception;

    /**
     * Check if the given value is valid instance of this type. If this returns
     * true, {@link #getIncreasedValue(String)} and
     * {@link #getDecreasedValue(String)} must be able to compute a result from
     * this value.
     * 
     * @param value
     * @return
     */
    public boolean isValidForType(String value) {
        return valueRegexp.test(value);
    }

    /**
     * Check if the given value is a valid, increased value. The given string is
     * guaranteed to be valid for this type.
     * 
     * @param value
     * @return true is the given value is a valid value in respect to the
     *         maximum value.
     */
    protected abstract boolean isSmallerThanMax(String value);

    /**
     * Check if the given value is a valid, decreased value. The given string is
     * guaranteed to be valid for this type.
     * 
     * @param value
     * @return true is the given value is a valid value in respect to the
     *         minumum value.
     */
    protected abstract boolean isLargerThanMin(String value);

    /**
     * Parse the given String value. Used for setting the maximum and minimum .
     * values. Should return null on an empty string.
     * 
     * @param value
     * @return
     */
    public abstract T parseStringValue(String value);

    public abstract S parseStepAmount(String value);

    public boolean canChangeFromTextBox() {
        return !isDisabled() && !isReadonly() && isManualInputAllowed();
    }

    /**
     * Increase the value of the field. Value is always checked for validity
     * first.
     */
    public void increaseValue() {
        String oldValue = textBox.getValue();

        if (isChangeable() && isValidForType(oldValue)) {
            try {
                String newValue = getIncreasedValue(oldValue);
                if (isSmallerThanMax(newValue)) {
                    textBox.setValue(newValue);

                    valueUpdateTimer.schedule(updateDelay);
                    valueUpdateTimer.setValue(newValue);
                }
            } catch (Exception e) {
                valueUpdateTimer.cancel();
                GWT.log("Exception when increasing value", e);
            }
        }
    }

    /**
     * Decrease the value of the field. Value is always checked for validity
     * first.
     */
    public void decreaseValue() {
        String oldValue = textBox.getValue();

        if (isChangeable() && isValidForType(oldValue)) {
            try {
                String newValue = getDecreasedValue(oldValue);
                if (isLargerThanMin(newValue)) {
                    textBox.setValue(newValue);

                    valueUpdateTimer.schedule(updateDelay);
                    valueUpdateTimer.setValue(newValue);
                }
            } catch (Exception e) {
                valueUpdateTimer.cancel();
                GWT.log("Exception when decreasing value", e);
            }
        }

    }

    /**
     * Set the value to the UI.
     * 
     * @param newValue
     */
    public void setValue(String newValue) {
        if (isValueValid(newValue)) {
            textBox.setValue(newValue);
            this.value = newValue;
        }
    }

    protected boolean isValueValid(String newValue) {
        return !safeEquals(newValue, value)
                && (isInvalidValuesAllowed() || (newValue != null && isValidForType(newValue)));
    }

    /*
     * From Guavas Object.equals
     */
    protected boolean safeEquals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public String getValue() {
        return value;
    }

    public void updateValueToServer(String newValue) {
        fireEvent(new StepperValueChangeEvent(newValue));
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        String newValue = event.getValue();
        if (isValueValid(newValue)) {
            value = newValue;
            valueUpdateTimer.cancel();
            updateValueToServer(newValue);

        } else {
            textBox.setValue(value);
        }
    }

    protected void enabledStateChanged() {
        if (isDisabled() || isReadonly()) {
            valueUpdateTimer.cancel();
        }

        textBox.setReadOnly(isDisabled() || isReadonly()
                || !isManualInputAllowed());
    }

    public HandlerRegistration addValueChangeHandler(
            ValueChangeHandler<String> handler) {
        return addHandler(handler, StepperValueChangeEvent.getType());
    }

    public void setTimerHasChangedValue(boolean timerHasChangedValue) {
        this.timerHasChangedValue = timerHasChangedValue;
    }

    public boolean isTimerHasChangedValue() {
        return timerHasChangedValue;
    }

    public boolean isChangeable() {
        return !isDisabled() && !isReadonly();
    }

    public boolean isMouseWheelEnabled() {
        return mouseWheelEnabled;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
        enabledStateChanged();
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public void setReadonly(boolean isReadonly) {
        this.isReadonly = isReadonly;
        enabledStateChanged();
    }

    public boolean isManualInputAllowed() {
        return isManualInputAllowed;
    }

    public void setManualInputAllowed(boolean isManualInputAllowed) {
        this.isManualInputAllowed = isManualInputAllowed;
    }

    public void setMouseWheelEnabled(boolean mouseWheelEnabled) {
        this.mouseWheelEnabled = mouseWheelEnabled;
    }

    public boolean isInvalidValuesAllowed() {
        return invalidValuesAllowed;
    }

    public void setInvalidValuesAllowed(boolean invalidValuesAllowed) {
        this.invalidValuesAllowed = invalidValuesAllowed;
        enabledStateChanged();
    }

    /**
     * Set the maximum possible value for this stepper. For
     * {@link #isValidForRange(String)}
     * 
     * @param value
     */
    public void setMaxValue(T value) {
        this.maxValue = value;
    }

    public T getMaxValue() {
        return maxValue;
    }

    /**
     * Set the minimum possible value for this stepper. For
     * {@link #isValidForRange(String)}
     * 
     * @param value
     */
    public void setMinValue(T value) {
        this.minValue = value;
    }

    public T getMinValue() {
        return minValue;
    }

    public void setStepAmount(S stepAmount) {
        this.stepAmount = stepAmount;
    }

    public S getStepAmount() {
        return this.stepAmount;
    }

    public TextBox getTextBox() {
        return textBox;
    }

    public boolean isValueFilteringEnabled() {
        return isValueFilteringEnabled;
    }

    public void setValueFilteringEnabled(boolean isValueFilteringEnabled) {
        this.isValueFilteringEnabled = isValueFilteringEnabled;
        textBox.setValueFilteringEnabled(isValueFilteringEnabled);
    }
}
