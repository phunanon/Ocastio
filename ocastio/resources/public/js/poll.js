//TODO: merge with ballot.js
const e = (el) => document.querySelector(el);

function UpdatePollDOM ()
{
  const sel = e("select");
  const option = sel.options[sel.selectedIndex];
  const hasNumWin = option.hasAttribute("data-num-win");
  e("div#num_win").style.display = hasNumWin ? "block" : "none";
  e("input#num_win").max = e("div#options").children.length - 1;
}

document.addEventListener("DOMContentLoaded", () => AddOption(e("div#options input")));
document.addEventListener("DOMContentLoaded", () => { UpdatePollDOM(); e('input[name="title"]').focus(); });

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
  UpdatePollDOM();
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
