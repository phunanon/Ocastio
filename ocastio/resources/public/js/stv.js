const e = (el) => document.querySelector(el);
const es = (el) => document.querySelectorAll(el);
let inputs, labels;

function NewCol (list, fg, bg) {
  console.log(list);
  list.forEach(el => {
    el.label.style.color = fg;
    el.label.style.backgroundColor = bg === undefined ? fg : bg;
  });
}

function HandleStv (e) {
  checked = inputs.filter(i => i.input.checked)
  vals  = checked.map(i => i.value);
  dupes = inputs.filter(i => vals.filter(v => v == i.value).length > 1);
  noputs= inputs.filter(i => !i.input.checked);
  done  = noputs.filter(i => vals.includes(i.value));
  todo  = noputs.filter(i => !done.includes(i));
  NewCol(done, "#cfc");
  NewCol(todo, "#fff");
  NewCol(dupes, "#fcc");
  NewCol(checked, "#fff", "#000");
}

document.addEventListener("DOMContentLoaded", () => {
  if (!e("form#vote").hasAttribute("data-stv")) return;
  inputs = Array.from(es("input")).filter(i => i.id.startsWith("oo"));
  inputs = inputs.map(i => {
    var x = {input: i, value: i.value, label: e(`[for=${i.id}]`)};
    return x;
  });
  inputs.forEach(i => i.input.addEventListener("input", HandleStv));
});
