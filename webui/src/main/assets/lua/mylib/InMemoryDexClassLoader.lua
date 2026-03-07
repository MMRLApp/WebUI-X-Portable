-- assets/lua/mylib/InMemoryDexClassLoader.lua
local std = require("mylib.std")

local IMDCL = java.import("dalvik.system.InMemoryDexClassLoader")

local M = {}

function M.new(file, parentClassLoader)
    parentClassLoader = parentClassLoader or context:getClassLoader()
    local buffer = ByteBuffer:wrap(file:readBytes())
    return IMDCL(buffer, parentClassLoader)
end

setmetatable(M, {
    __call = function(_, file, parent)
        return M.new(file, parent)
    end
})

return M
