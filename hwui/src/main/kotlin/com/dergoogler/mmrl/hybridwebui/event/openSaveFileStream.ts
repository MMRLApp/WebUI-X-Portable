export function openSaveFileStream(
  path: string,
  mimeType: string,
  chunkSize = 64 * 1024
): WritableStream<Uint8Array> {
  let aborted = false;
  let sentMetadata = false;

  const postMessageWithReply = (buffer: ArrayBuffer | string): Promise<string> =>
    new Promise((resolve, reject) => {
      if (aborted) return reject(new DOMException("Stream aborted", "AbortError"));

      const handler = (event: MessageEvent) => {
        window.SaveFileLauncher?.removeEventListener("message", handler);
        const data = event.data;
        if (typeof data !== "string") return reject(new Error("Invalid response from native"));
        if (data.startsWith("FAIL_")) reject(new Error(data));
        else resolve(data);
      };

      window.SaveFileLauncher?.addEventListener("message", handler);
      try {
        window.SaveFileLauncher?.postMessage(buffer);
      } catch (e) {
        window.SaveFileLauncher?.removeEventListener("message", handler);
        reject(new Error(`Failed to send message: ${e}`));
      }
    });

  const encodeMetadataChunk = (name: string, mime: string, chunk?: Uint8Array): ArrayBuffer => {
    const encoder = new TextEncoder();
    const nameBytes = encoder.encode(name);
    const typeBytes = encoder.encode(mime);
    const contentBytes = chunk ?? new Uint8Array();
    const buffer = new ArrayBuffer(4 + nameBytes.length + 4 + typeBytes.length + contentBytes.byteLength);
    const view = new DataView(buffer);
    let offset = 0;

    view.setUint32(offset, nameBytes.length);
    offset += 4;
    new Uint8Array(buffer, offset, nameBytes.length).set(nameBytes);
    offset += nameBytes.length;

    view.setUint32(offset, typeBytes.length);
    offset += 4;
    new Uint8Array(buffer, offset, typeBytes.length).set(typeBytes);
    offset += typeBytes.length;

    new Uint8Array(buffer, offset).set(contentBytes);
    return buffer;
  };

  return new WritableStream<Uint8Array>({
    async start() {},

    async write(chunk) {
      if (aborted) throw new DOMException("Stream aborted", "AbortError");
      if (!(chunk instanceof Uint8Array)) throw new TypeError("Chunk must be Uint8Array");

      // Split large chunk into smaller sub-chunks
      for (let offset = 0; offset < chunk.byteLength; offset += chunkSize) {
        const subChunk = chunk.subarray(offset, offset + chunkSize);
        const bufferToSend = !sentMetadata
          ? encodeMetadataChunk(path, mimeType, subChunk)
          : subChunk.buffer;

        sentMetadata = true;
        await postMessageWithReply(bufferToSend);
      }
    },

    async close() {
      if (!aborted) {
        // Signal Kotlin that all chunks are sent
        await postMessageWithReply("CLOSE");
        console.log("SaveFile WritableStream closed");
      }
    },

    abort(reason) {
      aborted = true;
      console.warn("SaveFile WritableStream aborted:", reason);
    },
  });
}