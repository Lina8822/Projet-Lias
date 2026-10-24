"""Passwords for local test fixtures; never provide a built-in credential."""
import os


def required_password(name="LIAS_TEST_PASSWORD"):
    value = os.environ.get(name, "")
    if len(value) < 12:
        raise ValueError(name + " is required (12+ characters)")
    return value
