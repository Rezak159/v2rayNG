package com.v2ray.ang.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsScrubberTest {

    @Test
    fun `share links keep only the scheme`() {
        val redacted = DiagnosticsScrubber.redact("import failed for vless://a1b2@example.com:443?sni=cdn.example.com#node")
        assertEquals("import failed for vless://<redacted>", redacted)
    }

    @Test
    fun `subscription urls are removed`() {
        val redacted = DiagnosticsScrubber.redact("GET https://sub.a4secure.xyz/app/sub/SECRETKEY failed")
        assertFalse(redacted!!.contains("SECRETKEY"))
    }

    @Test
    fun `config fields lose their values`() {
        val redacted = DiagnosticsScrubber.redact("""{"address": "203.0.113.7", "sni": "cdn.example.com", "password": "hunter2"}""")
        assertFalse(redacted!!.contains("203.0.113.7"))
        assertFalse(redacted.contains("cdn.example.com"))
        assertFalse(redacted.contains("hunter2"))
    }

    @Test
    fun `uuid and ip literals are masked`() {
        val redacted = DiagnosticsScrubber.redact("user 4a2b7c1d-1111-2222-3333-444455556666 via 198.51.100.42:8443")
        assertEquals("user <uuid> via <ip>", redacted)
    }

    @Test
    fun `host and port pairs are masked`() {
        val redacted = DiagnosticsScrubber.redact("connect failed: node1.example.net:2053")
        assertEquals("connect failed: <host>:<port>", redacted)
    }

    @Test
    fun `timestamps and versions survive`() {
        val text = "2026-08-27 12:34:56 core 2.3.5 started"
        assertEquals(text, DiagnosticsScrubber.redact(text))
    }

    @Test
    fun `stack trace class names survive`() {
        val text = "at com.v2ray.ang.core.CoreServiceManager.startCore(CoreServiceManager.kt:120)"
        assertEquals(text, DiagnosticsScrubber.redact(text))
    }

    @Test
    fun `long payloads are truncated`() {
        val redacted = DiagnosticsScrubber.redact("сбой подключения ".repeat(400))
        assertTrue(redacted!!.endsWith("…<truncated>"))
        assertTrue(redacted.length < 5_000)
    }

    @Test
    fun `null and empty input pass through`() {
        assertEquals(null, DiagnosticsScrubber.redact(null))
        assertEquals("", DiagnosticsScrubber.redact(""))
    }
}
