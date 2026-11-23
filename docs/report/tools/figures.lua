-- Đánh số hình/bảng theo chương, thay @fig:id/@tbl:id bằng "Hình c.n"/"Bảng c.n",
-- dựng lưới ảnh điện thoại từ div .phones và đánh dấu chương không đánh số.
-- Caption được tiền tố "Hình c.n: " để body.py đổi thành caption có trường SEQ.

local chapter, fig, tbl, unnumbered = 0, 0, 0, 0
local labels = {}

local PHONE_WIDTH = { [1] = "5cm", [2] = "5cm", [3] = "4.6cm" }

local function prefix_caption(caption, label)
  local blocks = caption.long
  if #blocks == 0 then
    caption.long = { pandoc.Plain { pandoc.Str(label .. ":") } }
    return caption
  end
  local first = blocks[1]
  first.content:insert(1, pandoc.Space())
  first.content:insert(1, pandoc.Str(label .. ":"))
  return caption
end

local function remember(id, label)
  if id ~= nil and id ~= "" then
    labels[id] = label
  end
end

local function phones_grid(div)
  fig = fig + 1
  local label = "Hình " .. chapter .. "." .. fig
  remember(div.identifier, label)
  local images = {}
  div:walk {
    Image = function(img)
      table.insert(images, img)
    end,
  }
  local width = PHONE_WIDTH[#images] or "4.6cm"
  local cells = {}
  for _, img in ipairs(images) do
    img.attributes.width = width
    img.attributes.height = nil
    img.caption = {}
    table.insert(cells, { pandoc.Plain { img } })
  end
  local aligns, widths = {}, {}
  for _ = 1, #images do
    table.insert(aligns, pandoc.AlignCenter)
    table.insert(widths, 0)
  end
  local grid = pandoc.utils.from_simple_table(pandoc.SimpleTable({}, aligns, widths, {}, { cells }))
  local text = div.attributes.caption or ""
  local caption = pandoc.Div(
    { pandoc.Para { pandoc.Str(label .. ": " .. text) } },
    { ["custom-style"] = "Image Caption" }
  )
  return { grid, caption }
end

local numbering = {
  traverse = "topdown",
  Header = function(h)
    if h.level ~= 1 then
      return nil
    end
    if h.classes:includes("unnumbered") then
      unnumbered = unnumbered + 1
      h.identifier = "unnumbered-" .. unnumbered
    else
      chapter = chapter + 1
      fig, tbl = 0, 0
    end
    return h
  end,
  Figure = function(f)
    fig = fig + 1
    local label = "Hình " .. chapter .. "." .. fig
    remember(f.identifier, label)
    f.caption = prefix_caption(f.caption, label)
    return f, false
  end,
  Table = function(t)
    tbl = tbl + 1
    local label = "Bảng " .. chapter .. "." .. tbl
    remember(t.identifier, label)
    t.caption = prefix_caption(t.caption, label)
    return t, false
  end,
  Div = function(d)
    if d.classes:includes("phones") then
      return phones_grid(d), false
    end
    return nil
  end,
}

local references = {
  Cite = function(c)
    local id = c.citations[1].id
    if id:match("^fig:") or id:match("^tbl:") then
      local label = labels[id]
      if label == nil then
        error("Không tìm thấy hình/bảng được tham chiếu: " .. id)
      end
      return pandoc.Str(label)
    end
    return nil
  end,
}

function Pandoc(doc)
  doc = doc:walk(numbering)
  return doc:walk(references)
end
