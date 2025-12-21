import { useEffect, useState } from 'react';
import api from '../api';
import Typeahead from './Typeahead';

export default function LocationPicker({ value, onChange }) {
  const [selectedProvince, setSelectedProvince] = useState(null);
  const [selectedDistrict, setSelectedDistrict] = useState(null);
  const [selectedSector, setSelectedSector] = useState(null);
  const [selectedCell, setSelectedCell] = useState(null);
  const [selectedVillage, setSelectedVillage] = useState(null);

  const updateFinalLocation = (overrideId) => {
    const id = overrideId ?? (selectedVillage?.id || selectedCell?.id || selectedSector?.id || selectedDistrict?.id || selectedProvince?.id || '');
    onChange(id || '');
  };

  // resolve initial value to selection objects (walk parents)
  useEffect(() => {
    if (!value) return;
    let cancelled = false;
    const loadHierarchy = async (id) => {
      try {
        const res = await api.get(`/locations/${id}`);
        if (!res?.data || cancelled) return;
        const node = res.data;
        // collect ancestors up to province
        let current = node;
        const map = { PROVINCE: null, DISTRICT: null, SECTOR: null, CELL: null, VILLAGE: null };
        map[current.type] = { id: current.id, name: current.name };
        while (current.parentId) {
          const pr = await api.get(`/locations/${current.parentId}`);
          if (!pr?.data) break;
          current = pr.data;
          map[current.type] = { id: current.id, name: current.name };
        }
        if (cancelled) return;
        setSelectedProvince(map.PROVINCE);
        setSelectedDistrict(map.DISTRICT);
        setSelectedSector(map.SECTOR);
        setSelectedCell(map.CELL);
        setSelectedVillage(map.VILLAGE);
      } catch (e) {
        // ignore
      }
    };
    loadHierarchy(value);
    return () => { cancelled = true; };
  }, [value]);

  return (
    <div className="form-grid">
      <div className="field">
        <label>Province</label>
        <Typeahead
          value={selectedProvince}
          onChange={(opt) => {
            setSelectedProvince(opt);
            setSelectedDistrict(null);
            setSelectedSector(null);
            setSelectedCell(null);
            setSelectedVillage(null);
            updateFinalLocation(opt?.id || '');
          }}
          fetchOptions={async (term) => {
            const res = await api.get('/locations', { params: { type: 'PROVINCE', name: term, page: 0, size: 10 } });
            return res.data.content || res.data || [];
          }}
          getOptionLabel={(o) => (o ? o.name : '')}
          placeholder="Type province"
        />
      </div>

      <div className="field">
        <label>District</label>
        <Typeahead
          value={selectedDistrict}
          onChange={(opt) => {
            setSelectedDistrict(opt);
            setSelectedSector(null);
            setSelectedCell(null);
            setSelectedVillage(null);
            updateFinalLocation(opt?.id || selectedProvince?.id || '');
          }}
          fetchOptions={async (term) => {
            if (!selectedProvince?.id) return [];
            const res = await api.get('/locations', { params: { type: 'DISTRICT', parentId: selectedProvince.id, name: term, page: 0, size: 10 } });
            return res.data.content || res.data || [];
          }}
          getOptionLabel={(o) => (o ? o.name : '')}
          placeholder="Type district"
          disabled={!selectedProvince}
        />
      </div>

      <div className="field">
        <label>Sector</label>
        <Typeahead
          value={selectedSector}
          onChange={(opt) => {
            setSelectedSector(opt);
            setSelectedCell(null);
            setSelectedVillage(null);
            updateFinalLocation(opt?.id || selectedDistrict?.id || selectedProvince?.id || '');
          }}
          fetchOptions={async (term) => {
            if (!selectedDistrict?.id) return [];
            const res = await api.get('/locations', { params: { type: 'SECTOR', parentId: selectedDistrict.id, name: term, page: 0, size: 10 } });
            return res.data.content || res.data || [];
          }}
          getOptionLabel={(o) => (o ? o.name : '')}
          placeholder="Type sector"
          disabled={!selectedDistrict}
        />
      </div>

      <div className="field">
        <label>Cell</label>
        <Typeahead
          value={selectedCell}
          onChange={(opt) => {
            setSelectedCell(opt);
            setSelectedVillage(null);
            updateFinalLocation(opt?.id || selectedSector?.id || selectedDistrict?.id || selectedProvince?.id || '');
          }}
          fetchOptions={async (term) => {
            if (!selectedSector?.id) return [];
            const res = await api.get('/locations', { params: { type: 'CELL', parentId: selectedSector.id, name: term, page: 0, size: 10 } });
            return res.data.content || res.data || [];
          }}
          getOptionLabel={(o) => (o ? o.name : '')}
          placeholder="Type cell"
          disabled={!selectedSector}
        />
      </div>

      <div className="field">
        <label>Village</label>
        <Typeahead
          value={selectedVillage}
          onChange={(opt) => {
            setSelectedVillage(opt);
            updateFinalLocation(opt?.id || selectedCell?.id || selectedSector?.id || selectedDistrict?.id || selectedProvince?.id || '');
          }}
          fetchOptions={async (term) => {
            if (!selectedCell?.id) return [];
            const res = await api.get('/locations', { params: { type: 'VILLAGE', parentId: selectedCell.id, name: term, page: 0, size: 10 } });
            return res.data.content || res.data || [];
          }}
          getOptionLabel={(o) => (o ? o.name : '')}
          placeholder="Type village"
          disabled={!selectedCell}
        />
      </div>
    </div>
  );
}
