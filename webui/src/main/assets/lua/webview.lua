-- webview.lua
-- WebView response helpers and handler registration

local java = require("wx:java")
local luajava = java.luajava

local M = {}

-- Stream helper

function M.stringToStream(s)
    local jstr = luajava.newInstance("java.lang.String", s)
    return luajava.newInstance("java.io.ByteArrayInputStream", jstr:getBytes("UTF-8"))
end

-- Response helpers

function M.htmlResponse(body, status)
    return WebResourceResponse("text/html",        "UTF-8", status or 200, "OK", {}, M.stringToStream(body))
end

function M.jsonResponse(body, status)
    return WebResourceResponse("application/json", "UTF-8", status or 200, "OK", {}, M.stringToStream(body))
end

function M.textResponse(body, status)
    return WebResourceResponse("text/plain",       "UTF-8", status or 200, "OK", {}, M.stringToStream(body))
end

function M.errorResponse(code, message)
    return WebResourceResponse("text/plain",       "UTF-8", code, message,  {}, M.stringToStream(message))
end

-- Handler registration

function M.registerPathHandler(path, handler, authority)
    _registerPathHandler:invoke(path, handler, authority)
end

function M.registerEventListener(objectName, event)
    _registerEventListener:invoke(objectName, event)
end

return M