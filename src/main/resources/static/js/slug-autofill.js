/**
 * Auto-fills a #slug field from a #name field as the user types, on any admin form that
 * has both. Stops overwriting as soon as the user edits the slug directly (or immediately,
 * in edit forms where the slug already has a value) so it never clobbers a deliberate change.
 */
(function () {
    function slugify(text) {
        return text
            .toString()
            .toLowerCase()
            .trim()
            .replace(/[^a-z0-9\s-]/g, '')
            .replace(/[\s_-]+/g, '-')
            .replace(/^-+|-+$/g, '');
    }

    document.addEventListener('DOMContentLoaded', function () {
        var nameInput = document.getElementById('name');
        var slugInput = document.getElementById('slug');
        if (!nameInput || !slugInput) {
            return;
        }

        var slugTouched = slugInput.value.trim() !== '';

        slugInput.addEventListener('input', function () {
            slugTouched = true;
        });

        nameInput.addEventListener('input', function () {
            if (!slugTouched) {
                slugInput.value = slugify(nameInput.value);
            }
        });
    });
})();
