const e = (el) => document.querySelector(el);

function UpdatePreview () {
  const importTxt = "\n"+ e("textarea").value;
  const lawsTxt = importTxt.split("\n#").filter(l => l != "");

  const laws = [];
  for (let l = 0; l < lawsTxt.length; ++l) {
    const txt = lawsTxt[l];
    const title = txt.match(/^#*(.+)/)[1];
    const hashes = txt.match(/^#+/);
    const level = hashes === null ? 0 : hashes[0].length;
    const body = txt.substr(txt.indexOf("\n"));
    laws.push({title: title, body: body, level: level});
  }

  let html = "";
  let prevLevel = 0;
  for (let l = 0; l < laws.length; ++l) {
    const law = laws[l];
    const closes = (prevLevel - law.level) + 1;
    for (let c = 0; c < closes; ++c)
      html += "</leaf>";
    prevLevel = law.level;
    html += `<leaf><leaftitle class="in">${law.title}</leaftitle><leafdesc><pre>${law.body}</pre></leafdesc>`;
  }

  e("tree").innerHTML = html;
}
