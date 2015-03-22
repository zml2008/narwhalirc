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
package ninja.leaping.narwhalirc.util;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spout.api.chat.ChatTemplate;
import org.spout.api.util.config.serialization.GenericType;
import org.spout.api.util.config.serialization.Serializer;

/**
 * Serializer to get chat templates
 */
public class ChatTemplateSerializer implements TypeSerializer {
    @Override
    public Object deserialize(TypeToken<?> genericType, ConfigurationNode o) {
        String value = String.valueOf(o);
        return ChatTemplate.fromFormatString(value);
    }

    @Override
    public void serialize(TypeToken<?> type, Object value, ConfigurationNode node) {
        return ((ChatTemplate) value).toFormatString();
    }

    @Override
    public boolean isApplicable(TypeToken<?> genericType) {
        return ChatTemplate.class.isAssignableFrom(genericType.getRawType());
    }
}
