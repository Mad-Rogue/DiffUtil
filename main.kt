import java.lang.Integer.min
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

enum class ACTION {
    SKIP,
    ADD,
    REMOVE
}

private data class Snake(
        val x: Int,
        val y: Int,
        val score: Int,
        val path: List<ACTION> = listOf()
)

class DiffResult(private val path: List<ACTION>) {
    fun apply(onInsert: ((position: Int, fromPosition: Int) -> Unit), onRemove: ((position: Int) -> Unit)) {
        var x = 0
        var y = 0
        for (action in path) {
            when (action) {
                ACTION.ADD -> {
                    onInsert.invoke(x, y)
                    ++x
                    ++y
                }
                ACTION.REMOVE -> {
                    onRemove.invoke(x)
                }
                ACTION.SKIP -> {
                    ++x
                    ++y
                }
            }
        }
    }
}

abstract class DiffCallback {
    abstract fun getOldListSize(): Int
    abstract fun getNewListSize(): Int
    abstract fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean
    private fun joinSnake(
            map: HashMap<String, Snake>,
            snakes: ArrayList<Snake>,
            sourceSnake: Snake,
            action: ACTION,
            w: Int,
            h: Int
    ): Boolean {
        var x = sourceSnake.x
        var y = sourceSnake.y
        var score = sourceSnake.score
        when (action) {
            ACTION.ADD -> {
                ++y
                ++score
            }
            ACTION.REMOVE -> {
                ++x
                ++score
            }
            ACTION.SKIP -> {
                ++x
                ++y
            }
        }
        val key = "${x}_$y"
        val item = map[key]
        if (item == null || score < item.score) {
            val snake = sourceSnake.copy(
                    x = x,
                    y = y,
                    path = ArrayList(sourceSnake.path).apply { add(action) },
                    score = score
            )
            snakes.add(snake)
            map[key] = snake
            item?.let {
                snakes.remove(it)
            }
        }
        return (x == w && y == h)
    }

    fun calculateDiff(diffCallback: DiffCallback): DiffResult {
        var snakes = arrayListOf<Snake>()
        snakes.add(Snake(0, 0, 0))

        val w = diffCallback.getOldListSize()
        val h = diffCallback.getNewListSize()
        var endFound = false
        while (!endFound) {
            val newSnakes = arrayListOf<Snake>()
            val map = hashMapOf<String, Snake>()
            for (snake in snakes) {
                when {
                    snake.x == w -> {
                        if (joinSnake(map, newSnakes, snake, ACTION.ADD, w, h)) endFound = true
                    }
                    snake.y == h -> {
                        if (joinSnake(map, newSnakes, snake, ACTION.REMOVE, w, h)) endFound = true
                    }
                    diffCallback.areItemsTheSame(snake.x, snake.y) -> {
                        if (joinSnake(map, newSnakes, snake, ACTION.SKIP, w, h)) endFound = true
                    }
                    else -> {
                        if (snake.x < w && joinSnake(map, newSnakes, snake, ACTION.REMOVE, w, h)) endFound = true
                        if (snake.y < h && joinSnake(map, newSnakes, snake, ACTION.ADD, w, h)) endFound = true
                    }
                }
            }
            snakes = newSnakes
        }
        var minScore = 0
        snakes.sortBy { snake ->
            minScore = if (minScore == 0) {
                snake.score
            } else {
                min(minScore, snake.score)
            }
            snake.score
        }
        val bestSnakes = snakes.filter { it.score == minScore }
        return DiffResult(
                if (bestSnakes.isEmpty()) {
                    emptyList()
                } else {
                    bestSnakes[0].path
                }
        )
    }
}


class CharDiffCallback(private val oldItems: List<Char>, private val newItems: List<Char>) : DiffCallback() {
    override fun getOldListSize(): Int = oldItems.size

    override fun getNewListSize(): Int = newItems.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}

@ExperimentalTime
fun main() {
    val oldItems = "If you continue to work on it, you're condition will get better.".toList()
    val newItems = "Well, things will get better after we get real food.".toList()
    val diffResult: DiffResult
    val time = measureTime {
        val diffCallback = CharDiffCallback(oldItems, newItems)
        diffResult = diffCallback.calculateDiff(diffCallback)
    }

    val time2 = measureTime {
        val items = ArrayList(oldItems)
        println("STR items = $items")
        diffResult.apply(
                onInsert = { position, fromPosition ->
                    items.add(position, newItems[fromPosition])
                    println("ADD items = $items")
                },
                onRemove = { position ->
                    items.removeAt(position)
                    println("REM items = $items")
                }
        )
        println("${items == newItems}, items = $items")
    }
    println("calculation time: $time")
    println("apply time: $time2")
}
