# Unicode Emoji catalog

- Version: 17.0
- Source: https://www.unicode.org/Public/17.0.0/emoji/emoji-test.txt
- Original file: `emoji-test-17.0.txt` (unmodified)
- SHA-256: `1d8a944f88d7952f7ef7c5167fef3c67995bcae24543949710231b03a201acda`
- License: Unicode License V3, see `LICENSE.txt`.
- Runtime allowlist: all 3,944 `fully-qualified` entries; `component`,
  `minimally-qualified`, and `unqualified` entries are excluded.
- No network calls are made to resolve or validate emoji at runtime.

## Existing keys

The existing clients map `HEART` to ❤️ (`u:2764-fe0f`) and `SMILE` to 😀
(`u:1f600`). Keep these original database values so v1 and v2 mutations share
one unique `(user_id, spot_record_id, reaction_type)` key. Other emoji use
canonical `u:<lowercase-codepoints>` storage values.

## Concurrent v1 and v2 operation

Both API contracts are available immediately in the same application:

- v1 `/api/feed` keeps HEART/SMILE requests and responses.
- v2 `/api/v2/feed` accepts every supported catalog key.
- Both use the same service, table, and unique constraint. The legacy two
  reactions map to the same rows from either API; other reactions are visible
  only in v2. Retire v1 separately after clients migrate.
- No activation flag or extra environment setting is required.

The pre-change enum-only application binary cannot read newly stored keys.
A rollback after v2 writes must retain the compatible string-based mapping.

## Updating

Download a reviewed Unicode version's original data file and license,
record its source and hash here, update `EmojiCatalog.version`, and update
the catalog tests. Keep previously accepted keys available for reads and
deletes. Verify that no existing key disappears before deployment.
