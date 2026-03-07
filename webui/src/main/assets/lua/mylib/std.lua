-- assets/lua/mylib/std.lua
-- core java imports and globals for library

local M = {}

M.String               = java.import("java.lang.String")
M.Boolean              = java.import("java.lang.Boolean")
M.ByteArrayInputStream = java.import("java.io.ByteArrayInputStream")
M.ByteArrayOutputStream= java.import("java.io.ByteArrayOutputStream")
M.BufferedInputStream  = java.import("java.io.BufferedInputStream")
M.Array                = java.import("java.lang.reflect.Array")
M.Byte                 = java.import("java.lang.Byte")
M.Object               = java.import("java.lang.Object")
M.ByteBuffer           = java.import("java.nio.ByteBuffer")
M.ModId                = java.import("com.dergoogler.mmrl.platform.model.ModId")

-- expose to globals for convenience if needed
_G.String                       = M.String
_G.Boolean                      = M.Boolean
_G.ByteArrayInputStream         = M.ByteArrayInputStream
_G.ByteArrayOutputStream        = M.ByteArrayOutputStream
_G.BufferedInputStream          = M.BufferedInputStream
_G.Array                        = M.Array
_G.Byte                         = M.Byte
_G.Object                       = M.Object
_G.ByteBuffer                   = M.ByteBuffer
_G.ModId                        = M.ModId

-- application context helpers
_G.options                      = _WX_OPTIONS
_G.context                      = _G.options:getContext()
_G.modId                        = _G.options:getModId()
_G.domain                       = _G.options:getDomain()

return M
