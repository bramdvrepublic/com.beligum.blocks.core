package com.beligum.blocks.core.validation.messages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.validation.messages.FeedbackMessage;

/**
 * Created by bas on 02.02.15.
 */
public class CustomFeedbackMessage implements FeedbackMessage
{
    private String messageKey;
    public FeedbackMessage.Level alertLevel;

    public CustomFeedbackMessage(String messageKey, FeedbackMessage.Level alertLevel)
    {
        this.messageKey = messageKey;
        this.alertLevel = alertLevel;
    }
    /**
     * @return the interpolated error message for this constraint violation
     */
    @Override
    public String getMessage()
    {
        return I18nFactory.instance().getDefaultResourceBundle().getMessage(messageKey);
    }
    /**
     * @return the non-interpolated error message for this constraint violation
     */
    @Override
    public String getMessageTemplate()
    {
        return messageKey;
    }
    @Override
    public Level getLevel()
    {
        return this.alertLevel;
    }
}
