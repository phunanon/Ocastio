const e = (el) => document.querySelector(el);
const isPoll = document.title.includes("oll");
const plural = (word, n) => n == 1 ? word : word +"s";

function UpdateBallotDOM ()
{
  const sel = e("select");
  const option = sel.options[sel.selectedIndex];
  const hasNumWin = option.hasAttribute("data-num-win");
  e("div#num_win").style.display = hasNumWin ? "block" : "none";
  let maxNumOpt;
  //if (isPoll) maxNumOpt = e("div#options").children.length - 1;
  //else        maxNumOpt = e("table#laws").rows.length;
  //e("input#num_win").max = maxNumOpt;
}

function UpdateValue (el, word, that)
{
  const val = e(that).value;
  word = plural(word, val);
  e(el).innerHTML = `${val} ${word}`;
}
setInterval("UpdateValue('#days', 'day', '#day'); UpdateValue('#hours', 'hour', '#hour')", 100);


//Poll related
function OptionKey (e, that)
{
  switch (e.keyCode) {
    case 13:
      AddOption(that);
      break;
    case 8:
      if (that.value.length == 0) {
        if (that.previousSibling !== null && that.previousSibling.previousSibling !== null)
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
}
function RemoveOption (el)
{
  if (el.nextSibling === null)
      el.previousSibling.focus();
    else
      el.nextSibling.focus();
    el.parentNode.removeChild(el)
}


//General
document.addEventListener("DOMContentLoaded", () => { UpdateBallotDOM(); });
document.addEventListener("DOMContentLoaded", () => { e('input[name="title"]').focus(); });
if (isPoll)
  document.addEventListener("DOMContentLoaded", () => AddOption(e("div#options input")));
