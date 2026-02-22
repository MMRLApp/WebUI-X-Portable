require("wx:std")

local sufile_import = java.import("com.dergoogler.mmrl.platform.file.SuFile")

local DEFAULT_BUFFER_SIZE = 8 * 1024

local M = {}

function _G.SuFile(...)
    local args = {...}
    local jarray = java.array(Object, #args)
    for i = 1, #args do
        jarray[i] = String(args[i])
    end
    return java.new(sufile_import, jarray)
end

function M.copyTo(input, output, bufferSize)
    bufferSize = bufferSize or DEFAULT_BUFFER_SIZE
    local bytesCopied = 0
    local chunk
    local buf = BufferedInputStream(input, bufferSize)
    -- read byte by byte as fallback
    local b = buf:read()
    while b ~= -1 do
        output:write(b)
        bytesCopied = bytesCopied + 1
        b = buf:read()
    end
    return bytesCopied
end

function M.readBytes(input)
    local available = input:available()
    local size = math.max(DEFAULT_BUFFER_SIZE, available)
    local buffer = ByteArrayOutputStream(size)
    M.copyTo(input, buffer)
    return buffer:toByteArray()
end

return M