require("wx:std")

local M = {}

function M.startsWith(s, prefix) return s:sub(1, #prefix) == prefix end
function M.endsWith(s, suffix)   return suffix == "" or s:sub(-#suffix) == suffix end
function M.trim(s)               return s:match("^%s*(.-)%s*$") end
function M.jsonString(s)         return '"' .. tostring(s):gsub('"', '\\"') .. '"' end
function M.stringToStream(s)     return ByteArrayInputStream(String(s):getBytes("UTF-8")) end

_G.startsWith       = M.startsWith
_G.endsWith         = M.endsWith
_G.trim             = M.trim
_G.jsonString       = M.jsonString
_G.stringToStream   = M.stringToStream

return M