/**
 * NarwhalIRC
 * Copyright (C) 2013 zml2008
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
        String value = String.valueOf(o);
        return ChatTemplate.fromFormatString(value);
    }

    @Override
    protected Object handleSerialize(GenericType type, Object value) {
        return ((ChatTemplate) value).toFormatString();
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
