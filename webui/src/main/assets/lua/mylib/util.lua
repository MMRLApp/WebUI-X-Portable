-- assets/lua/mylib/util.lua
local std  = require("mylib.std")
local str  = require("mylib.str")

-- Java imports used by utilities
local File            = java.import("java.io.File")
local Thread          = java.import("java.lang.Thread")
local URLEncoder       = java.import("java.net.URLEncoder")
local URLDecoder       = java.import("java.net.URLDecoder")
local JSONObject      = java.import("org.json.JSONObject")
local JSONArray       = java.import("org.json.JSONArray")

local M = {}

-- table helpers -----------------------------------------------------------
function M.keys(tbl)
    local out = {}
    for k,_ in pairs(tbl) do table.insert(out, k) end
    return out
end

function M.values(tbl)
    local out = {}
    for _,v in pairs(tbl) do table.insert(out, v) end
    return out
end

function M.merge(dest, ...)
    for _,src in ipairs({...}) do
        for k,v in pairs(src) do dest[k] = v end
    end
    return dest
end

function M.deepcopy(obj, seen)
    if type(obj) ~= "table" then return obj end
    if seen and seen[obj] then return seen[obj] end
    local s = seen or {}
    local res = {}
    s[obj] = res
    for k,v in pairs(obj) do res[M.deepcopy(k, s)] = M.deepcopy(v, s) end
    setmetatable(res, M.deepcopy(getmetatable(obj), s))
    return res
end

-- file & path ------------------------------------------------------------
function M.fileExists(path)
    return File(path):exists()
end

function M.isDirectory(path)
    return File(path):isDirectory()
end

function M.readFile(path, charset)
    charset = charset or "UTF-8"
    local fh = io.open(path, "rb")
    if not fh then return nil, "cannot open "..path end
    local data = fh:read("*a")
    fh:close()
    return data
end

function M.writeFile(path, data, mode)
    mode = mode or "wb"
    local fh = io.open(path, mode)
    if not fh then return nil, "cannot open "..path end
    fh:write(data)
    fh:close()
    return true
end

function M.pathJoin(...)
    local sep = "/"
    local parts = {...}
    return table.concat(parts, sep)
end

-- JSON helpers -----------------------------------------------------------
function M.toJson(tbl)
    -- JSONObject will traverse a Lua table automatically
    return JSONObject(tbl):toString()
end

function M.fromJson(str)
    return JSONObject(str)
end

-- misc ------------------------------------------------------------------
function M.urlEncode(s)
    return URLEncoder:encode(s, "UTF-8")
end

function M.urlDecode(s)
    return URLDecoder:decode(s, "UTF-8")
end

function M.sleep(ms)
    Thread:sleep(ms)
end

-- expose some useful bits globally if you like
_G.util = M

return M
