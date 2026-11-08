package com.ivy.data.backup

import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.repository.WealthRepository
import com.ivy.data.testResource
import io.mockk.mockk
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AiImportContractTest {
    private val json = Json
    private fun example() = json.parseToJsonElement(AiImportContract.guide.substringAfter("MINIMAL FICTIONAL EXAMPLE (do not reuse its IDs for real data):").substringBefore("JSON SCHEMA:").trim()).jsonObject
    private fun reject(change: (MutableMap<String, JsonElement>) -> Unit) {
        val root = example().toMutableMap(); change(root)
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare(JsonObject(root).toString()) }
    }
    private fun transaction(root: MutableMap<String, JsonElement>, change: (MutableMap<String, JsonElement>) -> Unit) {
        val tx = root.getValue("transactions").jsonArray.single().jsonObject.toMutableMap(); change(tx)
        root["transactions"] = JsonArray(listOf(JsonObject(tx)))
    }
    @Test fun `copied example decodes through production contract and serializer`() {
        val preview = AiImportContract.prepare(example().toString())
        assertEquals(1, preview.counts["Transactions"])
        assertEquals(45.5, preview.data.transactions.single().amount, 0.0)
        assertEquals(preview.data, KotlinxSerializationModule.provideJson().decodeFromString(IvyWalletCompleteData.serializer(), preview.payload))
    }
    @Test fun `schema is derived from backup serial names and required IDs`() {
        val schema = json.parseToJsonElement(AiImportContract.schema).jsonObject
        val properties = schema.getValue("properties").jsonObject
        assertFalse(properties.containsKey("settings")); assertFalse(properties.containsKey("sharedPrefs"))
        val tag = properties.getValue("tags").jsonObject.getValue("items").jsonObject
        assertTrue(tag.getValue("properties").jsonObject.containsKey("creationTime"))
        assertFalse(tag.getValue("properties").jsonObject.containsKey("dateTime"))
        val tx = properties.getValue("transactions").jsonObject.getValue("items").jsonObject
        assertTrue(tx.getValue("required").jsonArray.contains(JsonPrimitive("id")))
        assertTrue(AiImportContract.guide.endsWith(AiImportContract.schema))
        // Export public, generated contract for review; never wallet records or credentials.
        File("build/ai-import").mkdirs()
        File("build/ai-import/jvy-ai-import-v1.schema.json").writeText(AiImportContract.schema)
        File("build/ai-import/jvy-ai-conversion-guide-v1.md").writeText(AiImportContract.guide)
        val docs = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "docs/import-compatibility") }.first { File(it, "AI-CONVERSION.md").isFile }
        assertEquals("Published schema snapshot must match production", json.parseToJsonElement(AiImportContract.schema),
            json.parseToJsonElement(File(docs, "jvy-ai-import-v1.schema.json").readText()))
        assertEquals("Copyable guide snapshot must match production", AiImportContract.guide.trim(),
            File(docs, "jvy-ai-conversion-guide-v1.md").readText().trim())
    }
    @Test fun `unknown fields and app preferences are rejected not ignored`() {
        reject { it["settings"] = JsonArray(emptyList()) }
        reject { it["inventedTotal"] = JsonPrimitive(100) }
        reject { transaction(it) { tx -> tx["mystery"] = JsonPrimitive("lost data") } }
    }
    @Test fun `missing generated IDs cannot create fresh duplicates on every import`() {
        reject { transaction(it) { tx -> tx.remove("id") } }
    }
    @Test fun `missing account currency and orphan references are rejected`() {
        reject { root -> root["accounts"] = JsonArray(emptyList()) }
        reject { root -> val a = root.getValue("accounts").jsonArray.single().jsonObject.toMutableMap(); a.remove("currency"); root["accounts"] = JsonArray(listOf(JsonObject(a))) }
        reject { transaction(it) { tx -> tx["accountId"] = JsonPrimitive("11000000-0000-4000-8000-000000000099") } }
        reject { transaction(it) { tx -> tx["categoryId"] = JsonPrimitive("22000000-0000-4000-8000-000000000099") } }
    }
    @Test fun `wrong types negative amounts unsupported enums and missing dates are rejected`() {
        reject { transaction(it) { tx -> tx["amount"] = JsonPrimitive(-12) } }
        reject { transaction(it) { tx -> tx["amount"] = JsonPrimitive("45.50") } }
        reject { transaction(it) { tx -> tx["type"] = JsonPrimitive("DEBIT") } }
        reject { transaction(it) { tx -> tx["dateTime"] = JsonPrimitive("2026-10-25") } }
        reject { transaction(it) { tx -> tx.remove("dateTime") } }
    }
    @Test fun `duplicate IDs and malformed UUIDs are rejected`() {
        reject { it["transactions"] = JsonArray(it.getValue("transactions").jsonArray + it.getValue("transactions").jsonArray) }
        reject { transaction(it) { tx -> tx["id"] = JsonPrimitive("1-1-1-1-1") } }
    }
    @Test fun `broken transfers are rejected before writes`() {
        reject { transaction(it) { tx -> tx["type"] = JsonPrimitive("TRANSFER") } }
        reject { transaction(it) { tx -> tx["toAccountId"] = tx.getValue("accountId") } }
    }
    @Test fun `valid transfer keeps both currency amounts and is one record`() {
        val root = example().toMutableMap()
        val account = root.getValue("accounts").jsonArray.single().jsonObject.toMutableMap()
        account["id"] = JsonPrimitive("11000000-0000-4000-8000-000000000002"); account["currency"] = JsonPrimitive("USD")
        root["accounts"] = JsonArray(root.getValue("accounts").jsonArray + JsonObject(account))
        transaction(root) { tx -> tx["type"] = JsonPrimitive("TRANSFER"); tx.remove("categoryId"); tx["toAccountId"] = account.getValue("id"); tx["toAmount"] = JsonPrimitive(12.39) }
        val tx = AiImportContract.prepare(JsonObject(root).toString()).data.transactions.single()
        assertEquals(45.5, tx.amount, 0.0); assertEquals(12.39, tx.toAmount!!, 0.0)
    }
    @Test fun `blank transcript and oversized input are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare("```json\n{}\n```") }
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare(" ".repeat(AiImportContract.MAX_BYTES.toInt() + 1)) }
    }
    @Test fun `duplicate object keys escaped duplicate keys and excessive nesting are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare("{\"accounts\":[],\"accounts\":[]}") }
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare("{\"accounts\":[],\"acco\\u0075nts\":[]}") }
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare("[".repeat(1000) + "]".repeat(1000)) }
    }
    @Test fun `marketing fixture decodes and passes actual wealth validation`() {
        val data = json.decodeFromString(IvyWalletCompleteData.serializer(), testResource("backups/jvy-marketing-2026-10.json").readText())
        assertEquals(6, data.accounts.size); assertEquals(61, data.transactions.size)
        assertEquals(3, data.goals.size); assertEquals(4, data.goalAllocations.size)
        assertEquals(12, data.wealth!!.items.size)
        WealthRepository(mockk(relaxed = true)).apply { validate(data.wealth!!); validateReferences(data.wealth!!) }
        // AI profile deliberately excludes demo settings and fabricated completeness reviews.
        val profile = json.parseToJsonElement(testResource("backups/jvy-marketing-2026-10.json").readText()).jsonObject.toMutableMap()
        profile.remove("settings"); profile.remove("sharedPrefs")
        profile["wealth"] = JsonObject(profile.getValue("wealth").jsonObject.toMutableMap().apply { put("reviews", JsonArray(emptyList())) })
        assertEquals(61, AiImportContract.prepare(JsonObject(profile).toString()).data.transactions.size)
    }
    @Test fun `AI generated completion claims are rejected`() {
        val fixture = json.parseToJsonElement(testResource("backups/jvy-marketing-2026-10.json").readText()).jsonObject
        reject { it["wealth"] = fixture.getValue("wealth") }
    }

    @Test fun `cross currency loan payment needs actual loan currency amount`() {
        val root = example().toMutableMap()
        val aedAccount = root.getValue("accounts").jsonArray.single().jsonObject
        val usdAccount = JsonObject(aedAccount.toMutableMap().apply {
            put("id", JsonPrimitive("11000000-0000-4000-8000-000000000002")); put("currency", JsonPrimitive("USD"))
        })
        root["accounts"] = JsonArray(listOf(aedAccount, usdAccount))
        root["loans"] = json.parseToJsonElement("""[{"id":"66000000-0000-4000-8000-000000000001","name":"USD loan","type":"BORROW","amount":100,"accountId":"11000000-0000-4000-8000-000000000002"}]""")
        val record = json.parseToJsonElement("""{"id":"99000000-0000-4000-8000-000000000001","loanId":"66000000-0000-4000-8000-000000000001","amount":45.50,"dateTime":1792918800000,"accountId":"11000000-0000-4000-8000-000000000001"}""").jsonObject.toMutableMap()
        root["loanRecords"] = JsonArray(listOf(JsonObject(record)))
        assertThrows(IllegalArgumentException::class.java) { AiImportContract.prepare(JsonObject(root).toString()) }
        record["convertedAmount"] = JsonPrimitive(12.39)
        root["loanRecords"] = JsonArray(listOf(JsonObject(record)))
        assertEquals(12.39, AiImportContract.prepare(JsonObject(root).toString()).data.loanRecords.single().convertedAmount!!, 0.0)
    }

    @Test fun `linked wealth cannot reinterpret an account in a different currency`() {
        reject { it["wealth"] = json.parseToJsonElement("""{"items":[{"id":"example-wealth","name":"Linked bank","kind":"CASH","currency":"USD","source":"ACCOUNT","sourceId":"11000000-0000-4000-8000-000000000001","createdDate":"2026-10-01"}]}""") }
    }

    @Test fun `goal metadata cannot default to a new current timestamp on each import`() {
        reject { it["goals"] = json.parseToJsonElement("""[{"id":"44000000-0000-4000-8000-000000000001","name":"Laptop","kind":"PURCHASE","targetAmount":1000}]""") }
    }
}
