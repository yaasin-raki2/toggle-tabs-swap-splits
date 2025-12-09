package history

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * TDD tests for History class.
 * Each test is written BEFORE the implementation.
 */
class HistoryTest {
    
    private lateinit var history: History<String>
    
    @BeforeEach
    fun setup() {
        history = History()
    }
    
    // === Phase 1: Basic push and navigation ===
    
    @Test
    fun `new history has no places`() {
        assertFalse(history.canGoBack())
        assertFalse(history.canGoForward())
    }
    
    @Test
    fun `after pushing one place, cannot go back`() {
        history.push("A")
        assertFalse(history.canGoBack())
    }
    
    @Test
    fun `after pushing two places, can go back`() {
        history.push("A")
        history.push("B")
        assertTrue(history.canGoBack())
    }
    
    @Test
    fun `back returns previous place`() {
        history.push("A")
        history.push("B")
        val result = history.back()
        assertEquals("A", result)
    }
    
    @Test
    fun `after going back, can go forward`() {
        history.push("A")
        history.push("B")
        history.back()
        assertTrue(history.canGoForward())
    }
    
    @Test
    fun `forward returns the place we came from`() {
        history.push("A")
        history.push("B")
        history.back()
        val result = history.forward()
        assertEquals("B", result)
    }
    
    @Test
    fun `cannot go back past beginning`() {
        history.push("A")
        history.push("B")
        history.back()
        assertFalse(history.canGoBack())
        assertNull(history.back())
    }
    
    @Test
    fun `cannot go forward past end`() {
        history.push("A")
        history.push("B")
        assertFalse(history.canGoForward())
        assertNull(history.forward())
    }
    
    @Test
    fun `pushing after going back clears forward history`() {
        history.push("A")
        history.push("B")
        history.push("C")
        history.back() // at B
        history.back() // at A
        history.push("D") // should clear B and C
        assertFalse(history.canGoForward())
    }
    
    @Test
    fun `current place is the last pushed or navigated to`() {
        history.push("A")
        assertEquals("A", history.current())
        history.push("B")
        assertEquals("B", history.current())
        history.back()
        assertEquals("A", history.current())
        history.forward()
        assertEquals("B", history.current())
    }
    
    // === Phase 2: Multiple back/forward ===
    
    @Test
    fun `can navigate through multiple places`() {
        history.push("A")
        history.push("B")
        history.push("C")
        history.push("D")
        
        assertEquals("C", history.back())
        assertEquals("B", history.back())
        assertEquals("A", history.back())
        assertNull(history.back())
        
        assertEquals("B", history.forward())
        assertEquals("C", history.forward())
        assertEquals("D", history.forward())
        assertNull(history.forward())
    }
    
    // === Phase 3: Duplicate handling ===
    
    @Test
    fun `pushing same place consecutively does not add duplicate`() {
        history.push("A")
        history.push("A")
        history.push("A")
        assertFalse(history.canGoBack()) // only one place
    }
    
    @Test
    fun `pushing same place non-consecutively is allowed`() {
        history.push("A")
        history.push("B")
        history.push("A")
        assertTrue(history.canGoBack())
        assertEquals("B", history.back())
    }
}
