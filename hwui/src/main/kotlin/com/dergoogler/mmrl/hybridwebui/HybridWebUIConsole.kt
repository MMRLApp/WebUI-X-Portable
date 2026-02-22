package com.dergoogler.mmrl.hybridwebui

interface HybridWebUIConsole {
    /**
     * Logs an error message to the console, similar to `console.error` in JavaScript.
     *
     * This function allows for logging error messages along with optional arguments that will be
     * stringified and appended to the main message. It's particularly useful for debugging and
     * reporting issues encountered during the execution of web UI interactions.
     *
     * The implementation typically delegates to the underlying WebView's console object.
     *
     * @param message The primary error message to log.
     * @param args A variable number of optional arguments that will be converted to strings and
     *             included in the log output. These can be `null`.
     *
     * @sample
     * // Log a simple error message
     * console.error("An unexpected error occurred.")
     *
     * // Log an error message with additional details
     * val errorCode = 500
     * val details = "Server communication failed"
     * console.error("Request failed:", "Error code:", errorCode.toString(), "Details:", details)
     *
     * // Log an error with a null argument (which will be stringified)
     * console.error("Value was null:", null)
     *
     * @see HybridWebUIConsole.error
     */
    fun error(message: String, vararg args: String?)


    /**
     * Logs an error message based on a [Throwable] to the console.
     *
     * This function is specifically designed to handle and log exceptions. It will typically
     * format the error, potentially including the stack trace, and output it to the console
     * using the underlying error logging mechanism (e.g., `console.error` in a WebView).
     *
     * This provides a convenient way to report caught exceptions for debugging purposes.
     *
     * @param throwable The [Throwable] (e.g., an Exception) to be logged. The function will
     *                  extract relevant information from this object to construct the error message.
     *
     * @sample
     * try {
     *     // Some operation that might throw an exception
     *     throw RuntimeException("Something went wrong during processing")
     * } catch (e: Exception) {
     *     // Log the caught exception
     *     console.error(e)
     * }
     *
     * @see HybridWebUIConsole.error
     */
    fun error(throwable: Throwable)

    /**
     * Logs a trace message to the console.
     *
     * This function is typically used for detailed logging during development and debugging.
     * In many logging frameworks, trace messages are the most verbose and are often disabled in production builds
     * to avoid performance overhead. This implementation specifically states it "only logs in debug mode".
     *
     * @param message The message string to be logged. This message will be output to the console.
     *
     * @sample
     * // Example of logging a trace message.
     * console.trace("Entering function X with parameters Y and Z.")
     *
     * @see HybridWebUIConsole.error
     * @see HybridWebUIConsole.info
     * @see HybridWebUIConsole.log
     * @see HybridWebUIConsole.warn
     */
    fun trace(message: String)

    /**
     * Logs an informational message to the console.
     *
     * This function is part of the [HybridWebUIConsole] interface and is used to output informational messages
     * for debugging or status updates. It's similar to `console.info` in web browsers.
     *
     * @param message The main informational message string.
     * @param args Optional. A variable number of additional string arguments that can be
     *             interpolated into the message or logged separately, depending on the
     *             console implementation.
     */
    fun info(message: String, vararg args: String?)

    /**
     * Logs a message to the console with optional arguments.
     *
     * This function provides a way to output informational messages to the web console,
     * similar to `console.log` in JavaScript. It can be used for debugging or
     * providing general information about the application's state or actions.
     *
     * @param message The main message string to log. This can include format specifiers
     *                (e.g., `%s`, `%d`) if `args` are provided.
     * @param args Optional. A variable number of string arguments that will be
     *             substituted into the `message` string if it contains format specifiers.
     *             If no format specifiers are present, these arguments will be appended
     *             to the message.
     *
     * @sample
     * // Logging a simple message
     * log("Application has started.")
     *
     * // Logging a message with arguments
     * val userName = "Alice"
     * val userAge = 30
     * log("User %s is %d years old.", userName, userAge.toString())
     *
     * // Logging a message where arguments are appended
     * log("Processing item:", "ItemA", "ItemB")
     * // Output might be: Processing item: ItemA ItemB
     */
    fun log(message: String, vararg args: String?)

    /**
     * Logs a warning message to the console.
     *
     * This function uses the `console.warn` method of the underlying [HybridWebUIConsole] instance to display a warning message.
     * It supports string formatting, allowing placeholders in the `message` string to be replaced by the `args` values.
     *
     * @param message The main warning message string. It can contain placeholders (e.g., `%s`, `%d`) for the arguments.
     * @param args A variable number of string arguments to be inserted into the `message` string.
     *             These arguments will replace the placeholders in the order they appear.
     *
     * @sample
     * // Logs a simple warning message.
     * warn("This is a warning.")
     *
     * // Logs a warning message with arguments.
     * warn("Warning: Operation %s failed with code %d.", "File_Save", 500)
     * // This would output something like: "Warning: Operation File_Save failed with code 500."
     * // (Note: The actual formatting depends on the `console.warn` implementation.)
     *
     * @see HybridWebUIConsole.warn
     */
    fun warn(message: String, vararg args: String?)
}