const defaultStreamOptions = {
    chunkSize: 1024 * 1024,
    signal: null,
};

async function wrapToReadableStream(inputStream, options = {}) {
    const mergedOptions = { ...defaultStreamOptions, ...options };

    return new Promise((resolve, reject) => {
        let input;
        try {
            input = inputStream;
            if (!input) {
                throw new Error("Failed to open file input stream");
            }
        } catch (error) {
            reject(error);
            return;
        }

        const abortHandler = () => {
            try {
                input?.close();
            } catch (error) {
                console.error("Error during abort cleanup:", error);
            }
            reject(new DOMException("The operation was aborted.", "AbortError"));
        };

        if (mergedOptions.signal) {
            if (mergedOptions.signal.aborted) {
                abortHandler();
                return;
            }
            mergedOptions.signal.addEventListener("abort", abortHandler);
        }

        const stream = new ReadableStream({
            async pull(controller) {
                try {
                    const chunkData = input.readChunk(mergedOptions.chunkSize);
                    if (!chunkData) {
                        controller.close();
                        cleanup();
                        return;
                    }

                    const chunk = JSON.parse(chunkData);
                    if (chunk && chunk.length > 0) {
                        controller.enqueue(new Uint8Array(chunk));
                    } else {
                        controller.close();
                        cleanup();
                    }
                } catch (error) {
                    cleanup();
                    controller.error(error);
                    reject(new Error("Error reading file chunk: " + error.message));
                }
            },
            cancel() {
                cleanup();
            },
        });

        function cleanup() {
            try {
                if (mergedOptions.signal) {
                    mergedOptions.signal.removeEventListener("abort", abortHandler);
                }
                input?.close();
            } catch (error) {
                console.error("Error during cleanup:", error);
            }
        }

        resolve(stream);
    });
}

export { defaultStreamOptions, wrapToReadableStream }