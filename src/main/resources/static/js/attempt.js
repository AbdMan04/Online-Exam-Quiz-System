// Attempt page (R14/R17): saves each answer as it is chosen and counts the time down.
// When the timer reaches zero the attempt form is submitted; the server also enforces the deadline.
(function () {
    const root = document.getElementById('attempt');
    if (!root) {
        return;
    }
    const answerUrl = root.dataset.answerUrl;
    const submitForm = document.getElementById('submit-form');
    const timer = document.getElementById('timer');
    const saveStatus = document.getElementById('save-status');
    let remaining = parseInt(root.dataset.remainingSeconds, 10);

    function showStatus(text, isError) {
        saveStatus.textContent = text;
        saveStatus.classList.toggle('error', Boolean(isError));
    }

    function autoSubmit() {
        submitForm.dataset.auto = 'true';
        submitForm.submit();
    }

    root.addEventListener('change', async (event) => {
        const input = event.target;
        if (input.type !== 'radio') {
            return;
        }
        const body = new URLSearchParams({questionId: input.dataset.questionId, optionId: input.value});
        try {
            const response = await fetch(answerUrl, {method: 'POST', body});
            const data = await response.json();
            if (response.ok) {
                showStatus('Answer saved');
            } else {
                showStatus(data.error, true);
            }
        } catch (error) {
            showStatus('Could not save the answer - check your connection', true);
        }
    });

    function tick() {
        const minutes = Math.floor(remaining / 60);
        const seconds = remaining % 60;
        timer.textContent = minutes + ':' + String(seconds).padStart(2, '0');
        if (remaining <= 0) {
            clearInterval(countdown);
            autoSubmit();
        }
        remaining--;
    }

    const countdown = setInterval(tick, 1000);
    tick();
})();
