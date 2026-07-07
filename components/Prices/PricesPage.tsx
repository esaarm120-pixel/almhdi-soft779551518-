import React, { useEffect, useState } from 'react'
import { useStorage } from '../lib/storage'

type Price = { id:string, name:string, before:number, discount:number, after:number, max:number, min:number }
function uid(){ return Math.random().toString(36).slice(2,9) }

export default function PricesPage(){
  const { load, save } = useStorage()
  const [items, setItems] = useState<Price[]>([])
  const [form, setForm] = useState({name:'', before:0, discount:0, max:0, min:0})

  useEffect(()=>{ (async ()=>{
    const saved = await load('prices') as Price[] | null
    if(saved) setItems(saved)
  })() },[])
  useEffect(()=>{ save('prices', items) },[items])

  const calcAfter = (before:number, discount:number)=> Math.round((before * (1 - discount/100))*100)/100

  const add = ()=>{
    if(!form.name) return
    const p: Price = { id: uid(), name: form.name, before: form.before, discount: form.discount, after: calcAfter(form.before, form.discount), max: form.max, min: form.min }
    setItems(prev=>[...prev, p])
    setForm({name:'', before:0, discount:0, max:0, min:0})
  }

  const updateField = (id:string, field:keyof Price, value:any)=>{
    setItems(prev=> prev.map(it=> it.id===id ? {...it, [field]: value, after: field==='before' || field==='discount' ? calcAfter(field==='before' ? value : it.before, field==='discount' ? value : it.discount) : it.after } : it))
  }

  const updateDiscount = (id:string, d:number)=>{
    updateField(id, 'discount', d)
  }

  return (
    <div>
      <h3>ج - الأسعار</h3>
      <div className="add">
        <input placeholder="اسم الصنف" value={form.name} onChange={e=>setForm({...form, name:e.target.value})} />
        <input type="number" placeholder="السعر قبل الخصم" value={String(form.before)} onChange={e=>setForm({...form, before: Number(e.target.value)})} />
        <input type="number" placeholder="نسبة الخصم" value={String(form.discount)} onChange={e=>setForm({...form, discount: Number(e.target.value)})} />
        <input type="number" placeholder="اقصى سعر بيع" value={String(form.max)} onChange={e=>setForm({...form, max: Number(e.target.value)})} />
        <input type="number" placeholder="ادنى سعر بيع" value={String(form.min)} onChange={e=>setForm({...form, min: Number(e.target.value)})} />
        <button onClick={add}>إضافة صنف</button>
      </div>

      <table>
        <thead>
          <tr><th>اسم الصنف</th><th>السعر قبل الخصم</th><th>نسبة الخصم</th><th>السعر بعد الخصم</th><th>اقصى</th><th>ادنى</th><th>تحرير</th></tr>
        </thead>
        <tbody>
          {items.map(it=> (
            <tr key={it.id}>
              <td><input value={it.name} onChange={e=>updateField(it.id,'name', e.target.value)} /></td>
              <td><input type="number" value={String(it.before)} onChange={e=>updateField(it.id,'before', Number(e.target.value))} /></td>
              <td><input type="number" value={String(it.discount)} onChange={e=>updateDiscount(it.id, Number(e.target.value))} /></td>
              <td>{it.after}</td>
              <td><input type="number" value={String(it.max)} onChange={e=>updateField(it.id,'max', Number(e.target.value))} /></td>
              <td><input type="number" value={String(it.min)} onChange={e=>updateField(it.id,'min', Number(e.target.value))} /></td>
              <td><button onClick={()=>{ /* placeholder for future edit modal */ }}>حفظ</button></td>
            </tr>
          ))}
        </tbody>
      </table>

      <style jsx>{`
        table{ width:100%; border-collapse:collapse; margin-top:12px }
        th, td{ border:1px solid #ddd; padding:8px }
        td input{ width:100%; box-sizing:border-box }
        .add{ display:flex; gap:8px; flex-wrap:wrap }
      `}</style>
    </div>
  )
}
