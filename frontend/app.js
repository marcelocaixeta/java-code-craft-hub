/* CodeCraftHub frontend — JS puro, CRUD contra a API REST.
 * Endpoints usados:
 *   GET    {base}          -> listar
 *   GET    {base}/stats    -> estatísticas
 *   GET    {base}/search?q -> busca (com fallback local)
 *   POST   {base}          -> criar
 *   PUT    {base}/{id}     -> atualizar (parcial ok)
 *   DELETE {base}/{id}     -> excluir
 */
const $ = (id) => document.getElementById(id);
const apiBaseInput = $("apiBase");
const listEl = $("list");
const msgEl = $("msg");
const form = $("courseForm");

const api = () => apiBaseInput.value.trim().replace(/\/$/, "");

function showMsg(text, ok = true) {
  msgEl.textContent = text;
  msgEl.className = "msg " + (ok ? "ok" : "err");
  clearTimeout(showMsg.t);
  showMsg.t = setTimeout(() => msgEl.classList.add("hidden"), 4000);
  msgEl.classList.remove("hidden");
}
const errMsg = async (res, fallback) => {
  try { const j = await res.json(); return j.error || j.message || fallback; }
  catch { return fallback; }
};
const badgeClass = (s) => s === "Completed" ? "badge done" : s === "In Progress" ? "badge doing" : "badge";
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));

/* ---------- leitura ---------- */
async function loadStats() {
  try {
    const r = await fetch(`${api()}/stats`);
    if (!r.ok) return;
    const s = await r.json();
    const by = s.by_status || {};
    $("stTotal").textContent = s.total ?? "–";
    $("stNotStarted").textContent = by["Not Started"] ?? 0;
    $("stInProgress").textContent = by["In Progress"] ?? 0;
    $("stCompleted").textContent = by["Completed"] ?? 0;
  } catch { /* stats é bônus: ignora se falhar */ }
}

async function loadCourses() {
  listEl.innerHTML = `<p class="muted">Carregando…</p>`;
  try {
    const q = $("search").value.trim();
    let courses;
    if (q) {
      const r = await fetch(`${api()}/search?q=${encodeURIComponent(q)}`);
      courses = r.ok ? await r.json()
        : (await (await fetch(api())).json()).filter(filterLocal(q)); // fallback local
    } else {
      courses = await (await fetch(api())).json();
    }
    render(courses);
  } catch (e) {
    listEl.innerHTML = `<p class="muted">Falha ao carregar. Confira se o backend está em <b>${esc(api())}</b> e tente Recarregar.</p>`;
  }
  loadStats();
}
const filterLocal = (q) => (c) => {
  const t = q.toLowerCase();
  return (c.name || "").toLowerCase().includes(t) || (c.description || "").toLowerCase().includes(t);
};

function render(courses) {
  if (!Array.isArray(courses) || courses.length === 0) {
    listEl.innerHTML = `<p class="muted">Nenhum curso. Adicione o primeiro no formulário.</p>`;
    return;
  }
  listEl.innerHTML = courses.map((c) => `
    <article class="item" data-id="${c.id}">
      <div class="item-top"><h3>${esc(c.name)}</h3><span class="${badgeClass(c.status)}">${esc(c.status)}</span></div>
      <p class="desc">${esc(c.description)}</p>
      <div class="meta">#${c.id} · meta ${esc(c.target_date)} · criado em ${esc(c.created_at)}</div>
      <div class="item-actions">
        <button class="btn secondary small" data-act="edit">Editar</button>
        <button class="btn danger small" data-act="del">Excluir</button>
      </div>
    </article>`).join("");
}

/* ---------- criar / editar ---------- */
form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const id = $("editId").value;
  const body = {
    name: $("fName").value.trim(),
    description: $("fDesc").value.trim(),
    target_date: $("fDate").value, // yyyy-mm-dd direto do <input type="date">
    status: $("fStatus").value,
  };
  $("submitBtn").disabled = true;
  try {
    const res = await fetch(id ? `${api()}/${id}` : api(), {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) throw new Error(await errMsg(res, "Erro ao salvar"));
    showMsg(id ? `Curso #${id} atualizado!` : "Curso adicionado!");
    resetForm();
    loadCourses();
  } catch (err) { showMsg(err.message, false); }
  finally { $("submitBtn").disabled = false; }
});

function startEdit(article) {
  const id = article.dataset.id;
  const item = {
    name: article.querySelector("h3").textContent,
    desc: article.querySelector(".desc").textContent,
  };
  const meta = article.querySelector(".meta").textContent; // "#1 · meta 2025-12-31 · ..."
  const date = (meta.match(/meta (\d{4}-\d{2}-\d{2})/) || [])[1] || "";
  const status = article.querySelector(".badge").textContent.trim();
  $("editId").value = id;
  $("fName").value = item.name;
  $("fDesc").value = item.desc;
  $("fDate").value = date;
  $("fStatus").value = status;
  $("formTitle").textContent = `Editar curso #${id}`;
  $("submitBtn").textContent = "Salvar";
  $("cancelEditBtn").classList.remove("hidden");
  window.scrollTo({ top: 0, behavior: "smooth" });
}
function resetForm() {
  form.reset(); $("editId").value = "";
  $("formTitle").textContent = "Adicionar curso";
  $("submitBtn").textContent = "Adicionar";
  $("cancelEditBtn").classList.add("hidden");
}
$("cancelEditBtn").addEventListener("click", resetForm);

/* ---------- excluir (delegação na lista) ---------- */
listEl.addEventListener("click", async (e) => {
  const btn = e.target.closest("button[data-act]");
  if (!btn) return;
  const article = btn.closest(".item");
  const id = article.dataset.id;
  if (btn.dataset.act === "edit") { startEdit(article); return; }
  if (!confirm(`Excluir o curso #${id}?`)) return;
  try {
    const res = await fetch(`${api()}/${id}`, { method: "DELETE" });
    if (!res.ok) throw new Error(await errMsg(res, "Erro ao excluir"));
    showMsg(`Curso #${id} excluído.`);
    loadCourses();
  } catch (err) { showMsg(err.message, false); }
});

/* ---------- busca com debounce + recarregar ---------- */
let deb;
$("search").addEventListener("input", () => { clearTimeout(deb); deb = setTimeout(loadCourses, 300); });
$("reloadBtn").addEventListener("click", loadCourses);
apiBaseInput.addEventListener("change", loadCourses);

loadCourses();
