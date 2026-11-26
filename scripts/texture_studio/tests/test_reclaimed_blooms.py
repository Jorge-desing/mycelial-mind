import json
import tempfile
import unittest
from pathlib import Path

from PIL import Image

from scripts.model_studio.block_item_3d import build_reclaimed_bloom_model, build_reclaimed_bloom_variant
from scripts.texture_studio.generators.reclaimed_blooms import PALETTES, generate_reclaimed_blooms


def _resolve_asset(rel_path: str) -> Path:
    for root in (
        Path('src/generated/resources/assets/mycelialmind'),
        Path('src/main/resources/assets/mycelialmind'),
    ):
        candidate = root / rel_path
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(f'Missing asset in generated or main resources: {rel_path}')


class ReclaimedBloomAssetsTest(unittest.TestCase):
    def test_generated_assets_and_models_are_consistent(self):
        resources = Path('src/main/resources/assets/mycelialmind')
        states = json.loads(_resolve_asset('blockstates/reclaimed_bloom.json').read_text())['variants']
        with tempfile.TemporaryDirectory() as directory:
            generate_reclaimed_blooms(directory, emit_pbr=True)
            for index, kind in enumerate(PALETTES):
                name = 'reclaimed_bloom' if kind == 'base' else f'reclaimed_bloom_{kind}'
                model = build_reclaimed_bloom_model() if kind == 'base' else build_reclaimed_bloom_variant(kind)
                stored = json.loads(_resolve_asset(f'models/block/{name}.json').read_text())
                self.assertEqual(model, stored)
                for mature in ('true', 'false'):
                    self.assertEqual(states[f'mature={mature},palette={index}']['model'], f'mycelialmind:block/{name}')
                item = json.loads(_resolve_asset(f'models/item/{name}.json').read_text())
                self.assertEqual(item['parent'], f'mycelialmind:block/{name}')
                for suffix, size in (('', (128, 128)), ('_particle', (16, 16)), ('_n', (128, 128)), ('_s', (128, 128))):
                    filename = f'reclaimed_bloom_{kind}{suffix}.png'
                    generated = Path(directory) / filename
                    actual = resources / 'textures/model_atlas' / filename
                    self.assertTrue(actual.is_file(), f'Missing {actual}')
                    with Image.open(generated) as gen_img, Image.open(actual) as act_img:
                        self.assertEqual(gen_img.size, size)
                        self.assertEqual(act_img.size, size)
                        self.assertEqual(gen_img.mode, act_img.mode)
                        self.assertEqual(gen_img.tobytes(), act_img.tobytes(), f'Pixel mismatch in {filename}')
                self.assertEqual(model['textures']['particle'], f'mycelialmind:model_atlas/reclaimed_bloom_{kind}_particle')
                parts = model['elements']
                neighbours = [set() for _ in parts]
                for first, part in enumerate(parts):
                    for second in range(first):
                        other = parts[second]
                        intersection = [min(part['to'][axis], other['to'][axis])
                                        - max(part['from'][axis], other['from'][axis])
                                        for axis in range(3)]
                        self.assertFalse(all(distance > 0 for distance in intersection), name)
                        if all(distance >= 0 for distance in intersection):
                            neighbours[first].add(second)
                            neighbours[second].add(first)
                connected = {0}
                pending = [0]
                while pending:
                    for neighbour in neighbours[pending.pop()] - connected:
                        connected.add(neighbour)
                        pending.append(neighbour)
                self.assertEqual(len(connected), len(parts), name)
                for part in parts:
                    for lower, upper in zip(part['from'], part['to']):
                        self.assertTrue(0 <= lower < upper <= 16)
                    for face in part['faces'].values():
                        self.assertEqual(face['texture'], '#atlas')
                        self.assertTrue(all(0 <= uv <= 16 for uv in face['uv']))


if __name__ == '__main__':
    unittest.main()
