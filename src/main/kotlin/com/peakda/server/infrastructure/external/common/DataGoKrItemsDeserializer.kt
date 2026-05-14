package com.peakda.server.infrastructure.external.common

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer

class DataGoKrItemsDeserializer(
    private val elementType: JavaType? = null,
) : JsonDeserializer<List<Any?>>(), ContextualDeserializer {
    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty?,
    ): JsonDeserializer<*> {
        val containerType: JavaType? = property?.type ?: ctxt.contextualType
        val resolved = containerType?.containedType(0)
            ?: ctxt.typeFactory.constructType(Any::class.java)
        return DataGoKrItemsDeserializer(resolved)
    }

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): List<Any?> {
        val element = elementType ?: ctxt.typeFactory.constructType(Any::class.java)
        val listType = ctxt.typeFactory.constructCollectionType(List::class.java, element)

        return when (p.currentToken) {
            JsonToken.START_ARRAY -> readList(ctxt, p, listType)
            JsonToken.START_OBJECT -> {
                val node: JsonNode = p.readValueAsTree()
                val itemNode = node.get("item")
                when {
                    itemNode == null || itemNode.isNull -> emptyList()
                    itemNode.isArray -> readTreeAsList(ctxt, itemNode, listType)
                    else -> listOf(ctxt.readTreeAsValue(itemNode, element))
                }
            }
            else -> emptyList()
        }
    }

    override fun getNullValue(ctxt: DeserializationContext): List<Any?> = emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun readList(
        ctxt: DeserializationContext,
        p: JsonParser,
        listType: JavaType,
    ): List<Any?> = ctxt.readValue(p, listType) as List<Any?>

    @Suppress("UNCHECKED_CAST")
    private fun readTreeAsList(
        ctxt: DeserializationContext,
        node: JsonNode,
        listType: JavaType,
    ): List<Any?> = ctxt.readTreeAsValue<Any>(node, listType) as List<Any?>
}
