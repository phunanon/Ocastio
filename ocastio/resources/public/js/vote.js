const es = (el) => document.querySelectorAll(el);
const e = (el) => es(el)[0];
const isPoll = document.title.includes("oll");
const plural = (word, n) => n == 1 ? word : word +"s";

function UpdateBallotDOM ()
{
  const sel = e("select");
  const option = sel.options[sel.selectedIndex];
  const hasNumWin = option.hasAttribute("data-num-win");
  const isScore = option.hasAttribute("data-is-score");
  const isMass = option.hasAttribute("data-is-mass");
  e("p#num_win").style.display  = hasNumWin ? "flex" : "none";
  e("p#majority").style.display = hasNumWin ? "none" : "flex";
  e("p#range").style.display = isScore ? "flex" : "none";
  e("p#limit").style.display = isMass || isScore ? "none" : "flex";
  let maxNumOpt;
  if (isPoll) maxNumOpt = e("div#options").children.length - 1;
  else        maxNumOpt = e("table#laws").rows.length;
  e("input#num_win").max = maxNumOpt == 0 ? 1 : maxNumOpt;
}

function UpdateValue (el, word, that)
{
  const val = e(that).value;
  word = plural(word, val);
  e(el).innerHTML = `${val} ${word}`;
}
setInterval("UpdateValue('#days', 'day', '#day'); UpdateValue('#hours', 'hour', '#hour')", 100);

function OptionsChanged () {
  e("#limit_num").value = e("#limit_num").max =
    es(isPoll ? "#options input" : "input[type=checkbox]:checked").length;
}

//Poll related
function OptionKey (e, that)
{
  const opt = e.keyCode != undefined ? e.keyCode : 13;
  switch (opt) {
    case 13:
      AddOption(that);
      break;
    case 8:
      if (that.value.length == 0) {
        if (that.previousSibling !== null)
          RemoveOption(that);
      } else return true;
    break;
    case 38:
      that.previousSibling.focus();
      break;
    case 40:
      that.nextSibling.focus();
      break;
    default:
      return true;
  }
  UpdateBallotDOM();
  return false;
}
function AddOption (el)
{
  const parent = el.parentNode;
  const clone = el.cloneNode();
  clone.value = "";
  clone.name = "opt" + (parseInt(clone.name.substr(3)) + 1);
  parent.insertBefore(clone, el.nextSibling);
  clone.focus();
  OptionsChanged();
}
function RemoveOption (el)
{
  if (el.nextSibling === null)
    el.previousSibling.focus();
  else
    el.nextSibling.focus();
  el.parentNode.removeChild(el);
  OptionsChanged();
}
function AddOptionLast () {
  const lastOpt = Array.from(es("#options input")).pop();
  AddOption(lastOpt);
  UpdateBallotDOM();
}


//General
document.addEventListener("DOMContentLoaded", () => { UpdateBallotDOM(); });
document.addEventListener("DOMContentLoaded", () => { e('input[name="title"]').focus(); });
