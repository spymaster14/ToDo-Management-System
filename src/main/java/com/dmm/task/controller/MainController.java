package com.dmm.task.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.dmm.task.data.entity.Tasks;
import com.dmm.task.data.repository.TasksRepository;
import com.dmm.task.form.TaskForm;
import com.dmm.task.service.AccountUserDetails;

@Controller
public class MainController {
	@Autowired
	private TasksRepository repo;

	@GetMapping("/main")
	public String main(Model model, @AuthenticationPrincipal AccountUserDetails user,
			@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

		// 2次元表になるので、ListのListを用意する
		List<List<LocalDate>> matrix = new ArrayList<>();

		// その月の1日のLocalDateを取得する
		LocalDate day;

		if (date == null) {
			day = LocalDate.now();
		} else {
			day = date;
		}

		day = LocalDate.of(day.getYear(), day.getMonthValue(), 1);
		LocalDate start = day;

		// 当月、先月、翌月を表記
		model.addAttribute("month", day.format(DateTimeFormatter.ofPattern("yyyy年MM月")));
		model.addAttribute("prev", day.minusMonths(1));
		model.addAttribute("next", day.plusMonths(1));

		// 曜日を表すDayOfWeekを取得
		DayOfWeek w = day.getDayOfWeek();

		// 今月のカレンダーの最初の日
		LocalDate cal_start = start.with(DayOfWeek.SUNDAY).minusDays(7);
		day = cal_start;

		// 今月の最終日を求める
		LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

		// 最終日の次の土曜日を求める
		LocalDate cal_end = end.getDayOfWeek() == DayOfWeek.SATURDAY ? end
				: end.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

		List<LocalDate> week = new ArrayList<>(); // 週の初め
		do {
			week.add(day);
			day = day.plusDays(1);
			w = day.getDayOfWeek();
			if (w == DayOfWeek.SUNDAY || day.isAfter(cal_end)) {
				matrix.add(new ArrayList<>(week)); // 新しいインスタンスを追加
				week.clear(); // weekを初期化
			}
		} while (day.isBefore(cal_end.plusDays(1))); // 最終日も含むように修正

		// 管理者は全員分のタスクを見えるようにする
		List<Tasks> list;

		if (user.getUsername().equals("admin")) {
			list = repo.findByDateBetweenAdmin(cal_start.atTime(0, 0), end.atTime(0, 0));
		} else {
			list = repo.findByDateBetween(cal_start.atTime(0, 0), end.atTime(0, 0), user.getName());
		}

		// 取得したデータをtasksに追加する
		MultiValueMap<LocalDate, Tasks> tasks = new LinkedMultiValueMap<LocalDate, Tasks>();

		for (Tasks task : list) {
			LocalDate d = task.getDate().toLocalDate();
			tasks.add(d, task);
		}

		model.addAttribute("matrix", matrix);
		model.addAttribute("tasks", tasks);

		return "main";
	}

	@GetMapping("/main/create/{date}")
	public String create(@DateTimeFormat(pattern = "yyyy-MM-dd") @PathVariable LocalDate date) {
		return "create";
	}

	// 投稿を作成
	@PostMapping("/main/create")
	public String create(Model model, @Validated TaskForm taskForm, BindingResult bindingResult,
			@AuthenticationPrincipal AccountUserDetails user) {
		// バリデーションの結果、エラーがあるかどうかチェック
		if (bindingResult.hasErrors()) {
			// エラーがある場合は投稿登録画面を返す
			List<Tasks> list = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
			model.addAttribute("tasks", list);
			model.addAttribute("taskForm", taskForm);
			return "/main";
		}

		Tasks task = new Tasks();
		task.setTitle(taskForm.getTitle());
		task.setName(user.getName());
		task.setText(taskForm.getText());
		task.setDate(taskForm.getDate().atTime(0, 0));
		task.setDone(false);

		repo.save(task);

		return "redirect:/main";
	}

	// 投稿を編集
	@GetMapping("/main/edit/{id}")
	public String Taskid(Model model, @PathVariable Integer id) {
		Tasks task = repo.getById(id);
		model.addAttribute("task", task);
		return "/edit";
	}

	@PostMapping("/main/edit/{id}")
	public String edit(Model model, @Validated TaskForm taskForm, BindingResult bindingResult,
			@PathVariable Integer id) {

		Tasks task = repo.getById(id);

		model.addAttribute("task", task);

		task.setTitle(taskForm.getTitle());
		task.setText(taskForm.getText());
		task.setDate(taskForm.getDate().atTime(0, 0));
		task.setDone(taskForm.isDone());

		repo.save(task);

		return "redirect:/main";
	}

	// 投稿を削除
	@PostMapping("/main/delete/{id}")
	public String delete(@PathVariable Integer id) {
		repo.deleteById(id);
		return "redirect:/main";
	}

}