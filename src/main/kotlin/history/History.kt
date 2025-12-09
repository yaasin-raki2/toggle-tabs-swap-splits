package history

class History<T> {
    private val items = mutableListOf<T>()
    private var currentIndex = -1

    fun push(item: T) {
        if (currentIndex >= 0 && items[currentIndex] == item) {
            return
        }
        while (items.size > currentIndex + 1) {
            items.removeAt(items.size - 1)
        }
        items.add(item)
        currentIndex = items.size - 1
    }

    fun canGoBack(): Boolean = currentIndex > 0

    fun canGoForward(): Boolean = currentIndex < items.size - 1

    fun back(): T? {
        if (!canGoBack()) return null
        currentIndex--
        return items[currentIndex]
    }

    fun forward(): T? {
        if (!canGoForward()) return null
        currentIndex++
        return items[currentIndex]
    }

    fun current(): T? {
        if (currentIndex < 0 || currentIndex >= items.size) return null
        return items[currentIndex]
    }

    fun clear() {
        items.clear()
        currentIndex = -1
    }

    fun size(): Int = items.size
}
