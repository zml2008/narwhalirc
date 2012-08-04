package com.zachsthings.narwhal.irc.util;

import org.spout.api.chat.ChatTemplate;
import org.spout.api.util.config.serialization.GenericType;
import org.spout.api.util.config.serialization.Serializer;

/**
 * Serializer to get chat templates
 */
public class ChatTemplateSerializer extends Serializer {
    @Override
    protected Object handleDeserialize(GenericType genericType, Object o) {
        String value = String.valueOf(0);
        return NarwhalIRCUtil.fromString(value);
    }

    @Override
    protected Object handleSerialize(GenericType type, Object value) {
        return ((ChatTemplate) value).getArguments().asString();
    }

    @Override
    public boolean isApplicable(GenericType genericType) {
        return ChatTemplate.class.isAssignableFrom(genericType.getMainType());
    }

    @Override
    protected int getParametersRequired() {
        return 0;
    }
}
