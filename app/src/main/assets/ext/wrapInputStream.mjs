import { wrapToReadableStream, defaultStreamOptions } from "https://mui.kernelsu.org/internal/assets/ext/wrapToReadableStream.mjs"

async function wrapInputStream(inputStream, options = {}) {
    const mergedOptions = { ...defaultStreamOptions, ...options };

    try {
        const stream = await wrapToReadableStream(inputStream, mergedOptions);

        return new Response(stream, {
            headers: {
                "Content-Type": "application/octet-stream",
            },
        });
    } catch (error) {
        throw new Error("wrapInputStream failed: " + error.message);
    }
}

export { wrapInputStream }