#!/usr/bin/env python3
"""Reject unsafe or parent-relative actions/upload-artifact paths."""
import argparse
import json
import pathlib
import re
import sys
from pathlib import PurePosixPath, PureWindowsPath

UPLOAD = re.compile(r"^(?P<indent> *)-\s*uses:\s*actions/upload-artifact(?:@|/)")
STEP = re.compile(r"^(?P<indent> *)-\s+")
KEY = re.compile(r"^(?P<indent> *)([A-Za-z0-9_-]+):(?:\s*(.*))?$")


def scalar_values(value: str) -> list[str]:
    value = value.strip()
    if not value or value in ("|", "|-", ">", ">-"):
        return []
    if value.startswith("["):
        try:
            parsed = json.loads(value)
            return [str(part) for part in parsed] if isinstance(parsed, list) else [value]
        except json.JSONDecodeError:
            return [value]
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        value = value[1:-1]
    return [part.strip().strip("\"'") for part in value.splitlines() if part.strip()]


def check_file(path: pathlib.Path) -> list[str]:
    lines = path.read_text(encoding="utf-8-sig").splitlines()
    errors: list[str] = []
    for start, line in enumerate(lines):
        match = UPLOAD.match(line)
        if not match:
            continue
        step_indent = len(match.group("indent"))
        end = next((i for i in range(start + 1, len(lines))
                    if (m := STEP.match(lines[i])) and len(m.group("indent")) == step_indent), len(lines))
        with_line = next((i for i in range(start + 1, end)
                          if (m := KEY.match(lines[i])) and m.group(2) == "with" and len(m.group("indent")) > step_indent), None)
        if with_line is None:
            errors.append(f"{path}:{start + 1}: upload-artifact step has no with: block")
            continue
        with_indent = len(KEY.match(lines[with_line]).group("indent"))
        with_end = next((i for i in range(with_line + 1, end)
                         if lines[i].strip() and (m := KEY.match(lines[i])) and len(m.group("indent")) <= with_indent), end)
        path_line = next((i for i in range(with_line + 1, with_end)
                          if (m := KEY.match(lines[i])) and m.group(2) == "path"), None)
        if path_line is None:
            errors.append(f"{path}:{with_line + 1}: upload-artifact step has no path")
            continue
        value_match = KEY.match(lines[path_line])
        path_indent = len(value_match.group("indent"))
        raw = value_match.group(3) or ""
        candidates = scalar_values(raw)
        if raw.strip() in ("|", "|-", ">", ">-"):
            for child in lines[path_line + 1:with_end]:
                if child.strip() and len(child) - len(child.lstrip()) <= path_indent:
                    break
                text = child.strip()
                if not text or text.startswith("#"):
                    continue
                candidates.extend(scalar_values(text[2:] if text.startswith("- ") else text))
        if not candidates:
            errors.append(f"{path}:{path_line + 1}: upload-artifact path is empty")
            continue
        for candidate in candidates:
            normalized = candidate.replace("\\", "/")
            if PurePosixPath(normalized).is_absolute() or PureWindowsPath(candidate).is_absolute():
                errors.append(f"{path}:{path_line + 1}: artifact path must stay inside the workspace: {candidate}")
            elif any(part in (".", "..") for part in normalized.split("/")):
                errors.append(f"{path}:{path_line + 1}: dot-segment artifact path is disallowed: {candidate}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+", help="Workflow files or directories to scan")
    args = parser.parse_args()
    workflows: list[pathlib.Path] = []
    for value in args.paths:
        path = pathlib.Path(value)
        workflows.extend(sorted(path.glob("*.yml")) + sorted(path.glob("*.yaml")) if path.is_dir() else [path])
    errors = [message for workflow in workflows for message in check_file(workflow)]
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"Checked upload-artifact paths in {len(workflows)} workflow(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
