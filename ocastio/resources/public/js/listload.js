const es = (el) => document.querySelectorAll(el);

function CreateBtn (value, onclick) {
  const b = document.createElement("input");
  b.type = "button";
  b.value = value;
  b.onclick = () => onclick(b);
  return b;
}

function ShowRest (btn, list) {
  for (const c of list.childNodes)
    c.style.display = "block";
  btn.style.display = "none";
}

function HideRest (list, num) {
  const numChild = list.childNodes.length;
  for (let c = num; c < numChild; ++c)
    list.childNodes[c].style.display = "none";
  list.appendChild(CreateBtn("show rest", btn => ShowRest(btn, list)));
}

document.addEventListener("DOMContentLoaded", () => {
  const lists = es("ul.list");
  for (const l of lists)
    if (l.childNodes.length > 6)
      HideRest(l, 6);
});
