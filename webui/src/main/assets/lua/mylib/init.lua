-- assets/lua/mylib/init.lua
local M = {}

M.std   = require("mylib.std")
M.str   = require("mylib.str")
M.webview = require("mylib.webview")
M.SuFile = require("mylib.SuFile")
M.InMemoryDexClassLoader = require("mylib.InMemoryDexClassLoader")
M.util = require("mylib.util")

M._VERSION = "1.0.0"
M._AUTHOR  = "Jimmy"

return M
