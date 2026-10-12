(() => {
  if (window.__melonExperienceInstalled) return;
  window.__melonExperienceInstalled = true;
  const css = document.createElement("link");
  css.rel = "stylesheet";
  css.href = "melon-experience.css";
  document.head.appendChild(css);

  function pageName() {
    return (window.location.pathname.split("/").pop() || "index.html").toLowerCase();
  }
  function installBottomNav() {
    if (!document.body || document.getElementById("melon-bottom-nav")) return;
    document.body.classList.add("has-melon-bottom-nav");
    const page = pageName();
    const nav = document.createElement("nav");
    nav.id = "melon-bottom-nav";
    nav.setAttribute("aria-label", "Main navigation");
    const items = [
      {href:"index.html", label:"Home", icon:"⌂", active:page==="index.html" || page===""},
      {href:"index.html#marketplaceHome", label:"Categories", icon:"▦", active:page==="index.html" && window.location.hash==="#marketplaceHome"},
      {href:"cart.html", label:"Cart", icon:"🛍", active:page==="cart.html" || page==="checkout.html"},
      {href:"wishlist.html", label:"Wishlist", icon:"♡", active:page==="wishlist.html"}
    ];
    nav.innerHTML = items.map(item => '<a href="' + item.href + '"' + (item.active ? ' aria-current="page"' : '') + '><span class="mm-nav-icon" aria-hidden="true">' + item.icon + '</span><span class="mm-nav-label">' + item.label + '</span></a>').join("");
    document.body.appendChild(nav);
  }

  function installChat() {
    if (!document.body || document.getElementById("melon-welp-chat")) return;
    const root = document.createElement("div");
    root.id = "melon-welp-chat";
    root.innerHTML = `
      <button id="melon-welp-launcher" type="button" aria-label="Open Welp shopping assistant" aria-expanded="false" title="Chat with Welp">🍉</button>
      <section id="melon-welp-panel" role="dialog" aria-label="Welp shopping assistant" aria-modal="false" hidden>
        <header class="mm-welp-header">
          <div class="mm-welp-avatar" aria-hidden="true">🍉</div>
          <div class="mm-welp-heading"><strong>Welp</strong><small>Your Melon Mart shopping buddy</small></div>
          <button id="melon-welp-close" type="button" aria-label="Close chat">×</button>
        </header>
        <div class="mm-welp-intro">Hiii! Need a little shopping help? Ask about products, budgets, or your cart. ✨</div>
        <div class="mm-welp-prompts" aria-label="Quick questions">
          <button type="button" data-melon-prompt="What products do you have?">Browse products</button>
          <button type="button" data-melon-prompt="Show me something under ₹500">Under ₹500</button>
          <button type="button" data-melon-prompt="Show my cart">My cart</button>
        </div>
        <div id="melon-welp-messages" aria-live="polite">
          <div class="mm-welp-message mm-welp-bot">Welcome in! 🛍️ Tell me what you're looking for and we'll find it together.</div>
        </div>
        <form id="melon-welp-form">
          <input id="melon-welp-input" type="text" maxlength="500" placeholder="Ask Welp anything…" autocomplete="off" aria-label="Message Welp" required>
          <button id="melon-welp-send" type="submit" aria-label="Send message">➤</button>
        </form>
      </section>`;
    document.body.appendChild(root);
    const launcher = root.querySelector("#melon-welp-launcher");
    const panel = root.querySelector("#melon-welp-panel");
    const close = root.querySelector("#melon-welp-close");
    const form = root.querySelector("#melon-welp-form");
    const input = root.querySelector("#melon-welp-input");
    const messages = root.querySelector("#melon-welp-messages");
    const send = root.querySelector("#melon-welp-send");

    function openChat(open) {
      panel.hidden = !open;
      launcher.setAttribute("aria-expanded", String(open));
      if (open) input.focus();
    }
    function addMessage(text, type) {
      const bubble = document.createElement("div");
      bubble.className = "mm-welp-message mm-welp-" + type;
      bubble.textContent = String(text || "");
      messages.appendChild(bubble);
      messages.scrollTop = messages.scrollHeight;
      return bubble;
    }
    window.openMelonChat = () => openChat(true);
    launcher.addEventListener("click", () => openChat(panel.hidden));
    close.addEventListener("click", () => openChat(false));
    root.querySelectorAll("[data-melon-prompt]").forEach(button => {
      button.addEventListener("click", () => {
        input.value = button.getAttribute("data-melon-prompt") || "";
        form.requestSubmit();
      });
    });
    document.addEventListener("keydown", event => { if (event.key === "Escape" && !panel.hidden) openChat(false); });
    form.addEventListener("submit", async event => {
      event.preventDefault();
      const message = input.value.trim();
      if (!message) return;
      addMessage(message, "user");
      input.value = "";
      input.disabled = true;
      send.disabled = true;
      const pending = addMessage("Welp is thinking… 🍉", "bot");
      try {
        const response = await fetch("api/welp", {
          method: "POST",
          headers: {"Content-Type":"application/json"},
          body: JSON.stringify({message})
        });
        let result = {};
        try { result = await response.json(); } catch (_) {}
        pending.remove();
        if (!response.ok) addMessage(result.reply || "Welp ran into a little hiccup. Please try again! 🍉", "bot");
        else addMessage(result.reply || "I'm not sure what to say yet — try asking another way! 🍉", "bot");
      } catch (error) {
        pending.remove();
        addMessage("I can't connect right now. Please try again in a moment. 💗", "bot");
      } finally {
        input.disabled = false;
        send.disabled = false;
        input.focus();
      }
    });
    const url = new URL(window.location.href);
    if (url.searchParams.get("openWelp") === "1") {
      openChat(true);
      url.searchParams.delete("openWelp");
      window.history.replaceState({}, "", url.toString());
    }
  }

  function boot() { installBottomNav(); installChat(); }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot, {once:true});
  else boot();
})();
