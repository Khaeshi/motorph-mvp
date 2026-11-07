(function () {
  'use strict';

  var token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
  var csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
  var status = document.getElementById('status');
  var checker = document.getElementById('checker');

  function say(message, ok) {
    status.textContent = message;
    status.className = ok ? 'ok' : 'err';
  }

  document.querySelectorAll('.salary-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var row = btn.closest('tr');
      var id = btn.getAttribute('data-id');
      var value = row.querySelector('.salary-input').value;

      if (!checker.value.trim()) {
        say('Enter the checker (approver) username first.', false);
        return;
      }

      var headers = {
        'Content-Type': 'application/json',
        'X-Checker-Id': checker.value.trim()
      };
      headers[csrfHeader] = token;

      fetch('/admin/employees/' + encodeURIComponent(id) + '/salary', {
        method: 'POST',
        credentials: 'same-origin',
        headers: headers,
        body: JSON.stringify({ basicSalary: value })
      }).then(function (res) {
        return res.json().catch(function () { return {}; }).then(function (data) {
          return { ok: res.ok, data: data };
        });
      }).then(function (r) {
        if (r.ok) {
          row.querySelector('.salary-cell').textContent = r.data.basicSalary;
          say('Salary updated.', true);
        } else {
          say((r.data.error || 'Request rejected.') + (r.data.reason ? ' (' + r.data.reason + ')' : ''), false);
        }
      }).catch(function () {
        say('Network error.', false);
      });
    });
  });
})();
