package com.dergoogler.mmrl.wx.util

object PermissionParser {
    @Throws(IllegalArgumentException::class)
    fun parse(input: Any): Int {
        return when (input) {

            is Int -> {
                validateMode(input)
                input
            }

            is String -> {
                val trimmed = input.trim()

                val mode = when {
                    // symbolic form: rwxr-xr-x
                    trimmed.length == 9 && trimmed.all { it in "rwx-" } ->
                        symbolicToInt(trimmed)

                    // octal string: "0755" or "644"
                    trimmed.all { it in '0'..'7' } ->
                        octalStringToInt(trimmed)

                    else -> error("Unknown permission format: $input")
                }

                validateMode(mode)
                mode
            }

            else -> error("Unsupported type: ${input::class}")
        }
    }

    private fun symbolicToInt(symbolic: String): Int {
        require(symbolic.length == 9)

        var result = 0

        for (i in 0 until 3) {
            val chunk = symbolic.substring(i * 3, i * 3 + 3)

            var digit = 0
            if (chunk[0] == 'r') digit += 4
            if (chunk[1] == 'w') digit += 2
            if (chunk[2] == 'x') digit += 1

            result = (result shl 3) or digit
        }

        return result
    }

    private fun octalStringToInt(octal: String): Int {
        require(octal.all { it in '0'..'7' })
        return octal.toInt(8)
    }

    private fun validateMode(mode: Int) {
        require(mode in 0..0xFFF) {
            "Invalid Unix mode: $mode"
        }
    }

    fun toOctal(mode: Int): String =
        mode.toString(8).padStart(4, '0')

    fun toSymbolic(mode: Int): String {
        var result = ""

        for (i in 2 downTo 0) {
            val d = (mode shr (i * 3)) and 0b111

            result += buildString {
                append(if (d and 4 != 0) 'r' else '-')
                append(if (d and 2 != 0) 'w' else '-')
                append(if (d and 1 != 0) 'x' else '-')
            }
        }

        return result
    }
}