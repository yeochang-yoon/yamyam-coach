"""Convert an externally supplied MFDS XLSX/CSV into one stage's food catalog.

Only Python's standard library is required. Raw source workbooks are never copied
into the repository. Missing nutrients are skipped, not silently replaced by zero.
"""
import argparse
import csv
from collections import Counter
import json
import math
from pathlib import Path
import posixpath
import re
import xml.etree.ElementTree as ET
from zipfile import ZipFile

NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
FIELDS = {"id": "식품코드", "name": "식품명", "calories": "에너지(kcal)",
          "protein": "단백질(g)", "carbs": "탄수화물(g)", "fat": "지방(g)"}


def xlsx_rows(path):
    with ZipFile(path) as archive:
        strings = []
        if "xl/sharedStrings.xml" in archive.namelist():
            with archive.open("xl/sharedStrings.xml") as stream:
                for _, element in ET.iterparse(stream, events=("end",)):
                    if element.tag.endswith("}si"):
                        strings.append("".join(element.itertext()))
                        element.clear()
        workbook = ET.fromstring(archive.read("xl/workbook.xml"))
        relation_id = workbook.find("s:sheets/s:sheet", NS).attrib[f"{{{REL}}}id"]
        relationships = ET.fromstring(archive.read("xl/_rels/workbook.xml.rels"))
        target = next(e.attrib["Target"] for e in relationships if e.attrib["Id"] == relation_id)
        sheet = target.lstrip("/") if target.startswith("/") else posixpath.normpath("xl/" + target)
        with archive.open(sheet) as stream:
            header = None
            for _, element in ET.iterparse(stream, events=("end",)):
                if not element.tag.endswith("}row"):
                    continue
                cells = {}
                for cell in element.findall("s:c", NS):
                    column = re.match(r"[A-Z]+", cell.attrib["r"])[0]
                    value = cell.findtext("s:v", default="", namespaces=NS)
                    if cell.attrib.get("t") == "s":
                        value = strings[int(value)]
                    elif cell.attrib.get("t") == "inlineStr":
                        value = "".join(cell.find("s:is", NS).itertext())
                    cells[column] = value
                if header is None:
                    header = cells
                else:
                    yield {label: cells.get(column, "") for column, label in header.items()}
                element.clear()


def convert(path):
    if path.suffix.lower() == ".xlsx":
        rows = xlsx_rows(path)
    elif path.suffix.lower() == ".csv":
        with path.open(encoding="utf-8-sig", newline="") as stream:
            rows = list(csv.DictReader(stream))
    else:
        raise ValueError("입력 형식은 XLSX 또는 UTF-8 CSV입니다.")
    foods, skipped, seen = [], Counter(), set()
    for row in rows:
        if not all(header in row for header in [*FIELDS.values(), "영양성분함량기준량"]):
            raise ValueError("필수 열 이름을 확인하세요: " + ", ".join(FIELDS.values()))
        if re.sub(r"\s", "", row["영양성분함량기준량"]).lower() != "100g":
            skipped["100g 기준 아님"] += 1
            continue
        try:
            food = {key: str(row[header]).strip() for key, header in FIELDS.items()}
            if not food["id"] or not food["name"]:
                raise ValueError()
            for key in ["calories", "protein", "carbs", "fat"]:
                food[key] = float(food[key].replace(",", ""))
                if not math.isfinite(food[key]) or food[key] < 0:
                    raise ValueError()
        except (ValueError, TypeError):
            skipped["식별 정보 또는 영양 값 누락/오류"] += 1
            continue
        if food["id"] in seen:
            skipped["중복 식품코드"] += 1
            continue
        seen.add(food["id"])
        foods.append(food)
    if not foods:
        raise ValueError("변환 가능한 100g 기준 음식이 없습니다.")
    return foods, skipped


def mysql_value(value):
    if isinstance(value, str):
        if "\\" in value or any(ord(char) < 32 for char in value):
            return "CONVERT(0x" + value.encode("utf-8").hex() + " USING utf8mb4)"
        return "'" + value.replace("'", "''") + "'"
    return str(value)


def mysql_insert(table, columns, rows, on_duplicate=""):
    statements = []
    for offset in range(0, len(rows), 250):
        values = []
        for row in rows[offset:offset + 250]:
            values.append("    (" + ", ".join(mysql_value(value) for value in row) + ")")
        statements.append(
            "INSERT INTO " + table + " (" + ", ".join(columns) + ") VALUES\n"
            + ",\n".join(values) + on_duplicate + ";"
        )
    return "\n\n".join(statements)


def mysql_foods_sql(foods, on_duplicate=""):
    columns = ("food_code", "food_name", "basis_amount", "basis_unit", "calories", "protein",
               "carbs", "fat")
    rows = []
    for food in foods:
        code, name = food["foodCode"], food["name"]
        if not code.isascii() or len(code) > 36 or len(name) > 150:
            raise ValueError(f"MySQL 식품코드 또는 음식명 형식을 확인하세요: {code}")
        rows.append((code, name, food["basisAmount"], food["basisUnit"],
                     food["calories"], food["protein"], food["carbs"], food["fat"]))
    return ("-- 기본 스키마를 선택한 후 UTF-8로 실행합니다.\nSTART TRANSACTION;\n\n"
            + mysql_insert("foods", columns, rows, on_duplicate) + "\n\nCOMMIT;\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--stage", choices=[f"pjt{i}" for i in (1, 2, 3, 4, 5, 7)], default="pjt1")
    parser.add_argument("--dry-run", action="store_true", help="검증 결과만 출력하고 파일은 변경하지 않음")
    args = parser.parse_args()
    foods, skipped = convert(args.source)
    print(json.dumps({"imported": len(foods), "skipped": dict(skipped)}, ensure_ascii=False, indent=2))
    if not args.dry_run:
        stage = Path(__file__).resolve().parents[1] / args.stage
        records = []
        for food in foods:
            record = dict(food)
            code = record.pop("id")
            record.update(foodCode=code, basisAmount=100, basisUnit="g")
            records.append(record)
        if args.stage in ("pjt5", "pjt7"):
            sql_folder = "db" if args.stage == "pjt7" else "sql"
            target = stage / sql_folder / "food-import.sql"
            payload = mysql_foods_sql(
                records, "\nON DUPLICATE KEY UPDATE food_code = foods.food_code")
            message = f"Generated {target.name} for {len(records)} foods."
        else:
            if args.stage == "pjt3":
                target = stage / "web/data/foods.json"
            else:
                target = stage / "src/main/resources/data/foods.json"
            existing = json.loads(target.read_text(encoding="utf-8"))
            merged = {food["foodCode"]: food for food in existing["foods"]}
            for record in records:
                merged.setdefault(record["foodCode"], record)
            payload = json.dumps({"foods": list(merged.values())}, ensure_ascii=False, indent=2) + "\n"
            message = f"Updated {args.stage}: {len(merged)} foods."
        temp = target.with_suffix(target.suffix + ".tmp")
        temp.write_text(payload, encoding="utf-8")
        temp.replace(target)
        print(message)
