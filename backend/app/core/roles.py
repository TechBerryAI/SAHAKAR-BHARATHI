"""Account roles. status stays active/disabled; role is a separate column."""

ROLE_PARTICIPANT = "participant"
ROLE_ADMIN = "admin"
ROLE_SUPER_ADMIN = "super_admin"

STAFF_ROLES = frozenset({ROLE_ADMIN, ROLE_SUPER_ADMIN})
ASSIGNABLE_ROLES = frozenset({ROLE_PARTICIPANT, ROLE_ADMIN})
