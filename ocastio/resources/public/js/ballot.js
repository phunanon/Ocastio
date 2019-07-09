function UpdateBallotDOM ()
{
  const sel = e("select");
  const option = sel.options[sel.selectedIndex];
  const hasNumWin = option.hasAttribute("data-num-win");
  e("div#num_win").style.display = hasNumWin ? "block" : "none";
  e("input#num_win").max = e("table#laws").rows.length;
}

document.addEventListener("DOMContentLoaded", () => { UpdateBallotDOM(); e('input[name="title"]').focus(); });

const e = (el) => document.querySelector(el);
