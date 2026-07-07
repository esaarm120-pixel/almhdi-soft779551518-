import React, { useEffect, useState } from 'react'
import { useStorage } from '../../lib/storage'

type Task = { id:string, text:string, date:string, status:'waiting'|'partial'|'done'|'completed' }

function uid(){ return Math.random().toString(36).slice(2,9) }

export default function TasksPage(){
  const { load, save } = useStorage()
  const [tasks, setTasks] = useState<Task[]>([])
  const [newText, setNewText] = useState('')

  useEffect(()=>{ (async ()=>{
    const saved = await load('tasks') as Task[] | null
    if(saved) setTasks(saved)
  })() },[])

  useEffect(()=>{ save('tasks', tasks) },[tasks])

  const addTask = ()=>{
    if(!newText.trim()) return
    const t: Task = { id: uid(), text: newText.trim(), date: new Date().toISOString(), status:'waiting' }
    setTasks(prev=>[...prev, t])
    setNewText('')
  }

  // transitions
  const markDone = (id:string)=> setTasks(prev=> prev.map(t=> t.id===id ? {...t, status:'completed'} : t))
  const markPartial = (id:string)=> setTasks(prev=> prev.map(t=> t.id===id ? {...t, status:'partial'} : t))
  const markCompletedFromPartial = (id:string)=> setTasks(prev=> prev.map(t=> t.id===id ? {...t, status:'completed'} : t))
  const moveToWaiting = (id:string)=> setTasks(prev=> prev.map(t=> t.id===id ? {...t, status:'waiting'} : t))
  const removeTask = (id:string)=> setTasks(prev=> prev.filter(t=> t.id!==id))

  const sections = {
    waiting: tasks.filter(t=> t.status==='waiting'),
    partial: tasks.filter(t=> t.status==='partial'),
    done: tasks.filter(t=> t.status==='done'),
    completed: tasks.filter(t=> t.status==='completed')
  }

  return (
    <div>
      <h3>أ - قسم المهمات</h3>

      <div className="add-area">
        <div className="add-input">
          <input placeholder="اكتب المهمة هنا..." value={newText} onChange={e=>setNewText(e.target.value)} />
          <button className="add-btn" onClick={addTask}>حفظ وادخال في قيد الانتظار</button>
        </div>
      </div>

      <div className="grid">
        <section>
          <h4 style={{color:'red'}}>2. مهمات قيد الانتظار</h4>
          {sections.waiting.length===0 && <div className="empty">لا توجد مهمات قيد الانتظار</div>}
          {sections.waiting.map(t=> (
            <div key={t.id} className="task red">
              <div className="text">{t.text} <small>— {new Date(t.date).toLocaleString()}</small></div>
              <div className="actions">
                <button title="تم إنجازها" className="circle done" onClick={()=>markDone(t.id)}>✔</button>
                <button title="تم إنجاز جزء" className="circle partial" onClick={()=>markPartial(t.id)}>◐</button>
              </div>
            </div>
          ))}
        </section>

        <section>
          <h4 style={{color:'orange'}}>3. مهمات تم انجازها جزئاً</h4>
          {sections.partial.length===0 && <div className="empty">لا توجد مهمات هنا</div>}
          {sections.partial.map(t=> (
            <div key={t.id} className="task orange">
              <div className="text">{t.text} <small>�� {new Date(t.date).toLocaleString()}</small></div>
              <div className="actions">
                <button className="circle done" title="نقل إلى المكتملة" onClick={()=>markCompletedFromPartial(t.id)}>✔</button>
                <button className="circle" title="إلغاء وإعادتها إلى قيد الانتظار" onClick={()=>moveToWaiting(t.id)}>↺</button>
              </div>
            </div>
          ))}
        </section>

        <section>
          <h4 style={{color:'green'}}>4. المهمات المكتملة</h4>
          {sections.completed.length===0 && <div className="empty">لا توجد مهمات مكتملة</div>}
          {sections.completed.map(t=> (
            <div key={t.id} className="task green">
              <div className="text">{t.text} <small>— {new Date(t.date).toLocaleString()}</small></div>
              <div className="actions">
                <button className="circle" title="حذف المهمة" onClick={()=>removeTask(t.id)}>🗑</button>
                <button className="circle" title="إعادة إلى قيد الانتظار" onClick={()=>moveToWaiting(t.id)}>↺</button>
              </div>
            </div>
          ))}
        </section>
      </div>

      <style jsx>{`
        .add-area{ margin-bottom:12px }
        .add-input{ display:flex; gap:8px }
        .add-input input{ flex:1; padding:8px }
        .add-btn{ padding:8px 12px }

        .grid{ display:grid; grid-template-columns:1fr; gap:12px }
        @media(min-width:900px){ .grid{ grid-template-columns: repeat(3,1fr) } }
        .task{ display:flex; justify-content:space-between; align-items:center; padding:8px; border-radius:6px; border:1px solid #eee; margin-bottom:6px }
        .task.red{ background:#ffecec; border-color:#ffcccc }
        .task.orange{ background:#fff3e0; border-color:#ffd0a8 }
        .task.green{ background:#ecffe9; border-color:#b7f0c9 }
        .actions{ display:flex; gap:8px }
        .circle{ width:36px; height:36px; border-radius:50%; border:none; display:inline-flex; align-items:center; justify-content:center; cursor:pointer; background:#bbb; color:white }
        .circle.done{ background:#2ecc71 }
        .circle.partial{ background:#f39c12 }
        .empty{ color:#666; font-style:italic; padding:6px }
      `}</style>
    </div>
  )
}
