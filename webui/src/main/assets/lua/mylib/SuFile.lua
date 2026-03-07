-- assets/lua/mylib/SuFile.lua
local std = require("mylib.std")

local sufile_import = java.import("com.dergoogler.mmrl.platform.file.SuFile")

local DEFAULT_BUFFER_SIZE = 8 * 1024

local SuFile = {}

function SuFile.new(...)
    local args = {...}
    local jarray = java.array(Object, #args)
    for i = 1, #args do
        jarray[i] = String(args[i])
    end
    return java.new(sufile_import, jarray)
end

function SuFile.copyTo(input, output, bufferSize)
    bufferSize = bufferSize or DEFAULT_BUFFER_SIZE
    local bytesCopied = 0
    local buf = BufferedInputStream(input, bufferSize)
    local b = buf:read()
    while b ~= -1 do
        output:write(b)
        bytesCopied = bytesCopied + 1
        b = buf:read()
    end
    return bytesCopied
end

function SuFile.readBytes(input)
    local size = math.max(DEFAULT_BUFFER_SIZE, input:available())
    local buffer = ByteArrayOutputStream(size)
    SuFile.copyTo(input, buffer)
    return buffer:toByteArray()
end

setmetatable(SuFile, {
    __call = function(_, ...)
        return SuFile.new(...)
    end
})

return SuFile
