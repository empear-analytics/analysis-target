# This Controller has the commonly used methods which is used among few reports
# Reports should inherit from this class if required.
class Tenant::Reports::ReportsController < ApplicationController
        before_action :authenticate_user!
  before_action :check_tenant, :switch_to_tenant_db
  before_action :is_current_organization_active?
  before_action :check_tenant_role_permission, except: [:list_of_custom_values]
        layout "tenant"
        include Tenant::Reports::ReportsHelper

  # Initialize the variables - hashes, etc, ... , needed for the index methods.
  # This method also sets default values for params shared across reports.
  # @param [Boolean] aged true if your using aged reports else false.
  # @example Initializing for Aged invoices
  #   __initialize(true) # in aged report controller.rb
  def __initialize(aged = false)
    # Initialize variables
    @final = {}
    @final_total_balance_period = {}
    @final_sub_acc_total_balance_period = {}
    balance_by_account_type = 0
    if params[:filter] == "false"
      session[:filter] = {}
      session[:compare_to] = ""
      session[:compare_periods] = ""
      session[:custom_field_analysis] = ""
      session[:balance_date] = ""
      session[:custom_field] = ""
      session[:custom_value] = ""
      session[:from] = ""
      session[:to] = ""
    end
    if (aged)
      params[:balance_date] = params[:balance_date].present? ? params[:balance_date].try(:to_date) : Date.current
    else
      params[:balance_date] = params[:balance_date].present? ? params[:balance_date] : (session[:balance_date].present? ? session[:balance_date] : Date.current.strftime("%b-%Y"))
    end
    session[:from] = params[:from] if params[:from].present?
    session[:to] = params[:to] if params[:to].present?
    session[:custom_field] = params[:custom_field] if params[:custom_field].present?
    session[:custom_field_analysis] = params[:custom_field_analysis] if params[:custom_field_analysis].present?
    session[:custom_value] = params[:custom_value] if params[:custom_value].present?
    session[:compare_periods] = params[:compare_periods] if params[:compare_periods].present?
    session[:compare_to] = params[:compare_to] if params[:compare_to].present?
    session[:balance_date] = params[:balance_date] if params[:balance_date].present?
    @custom_field_analysis = (session[:custom_field].present? && session[:custom_field_analysis] == 'true' && !session[:from].blank? && !session[:to].blank?) ? true : false
    @compare_periods = params[:compare_periods].present? ? params[:compare_periods] : session[:compare_periods]
    @compare_to = params[:compare_to].present? ? params[:compare_to] : session[:compare_to]
    @balance_date = (params[:balance_date].present? ? params[:balance_date] : session[:balance_date])
    # @balance_date = @balance_date.acts_like?(:date) ? @balance_date : Date.current.to_s
    @no_of_periods = (@compare_periods).present? ? @compare_periods.sub("Period ", "").to_i : 0
    if (!aged)
      if @custom_field_analysis
        @period_array = (session[:custom_value] == 'all' || session[:custom_value].blank?) ? CpnCustomField.includes(:cpn_custom_field_values).where(field_name: session[:custom_field]).pluck(:value).uniq.compact.collect{ |e| e ? e.strip : e }.uniq : [session[:custom_value]]
        @period_array.delete_if {|h| h.blank?}
      else
        @period_array = date_array(@compare_to, @compare_periods, @balance_date)
      end
    end
  end

  def new
    session[:report_query] ||= {}
    session[:report_query][:show_split] = params[:show_split] == 'true'
    head :ok
  end

  # Method returns the array of dates
  # Params::
  # 1.compare_to specifies what kind of period separation
  # 2.compare_periods specifies how many periods
  # 3.date is the from_date which is used to begin from.
  # this method is used in balance_sheet_controller, income_statement_controller etc.,
  def from_to_date(compare_to = '', period = '')
    final = {}
    if compare_to == "Previous Month"
      final['from_date'] = Date.parse(period).beginning_of_month.strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Year"
      final['from_date'] = (Date.parse(period).ago(1.year).end_of_month + 1).strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Quarter"
      final['from_date'] = (Date.parse(period).ago(3.month).end_of_month + 1).beginning_of_month.strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Week"
      final['from_date'] = (Date.parse(period).ago(6.days)).strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    elsif compare_to == "Previous Day"
      final['from_date'] = (Date.parse(period)).strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    elsif compare_to == ''
      final['from_date'] = get_last_fiscal_year(period)
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    end
    return final
  end


  def from_to_date_balance_sheet(compare_to = '', period = '')
    final = {}
    if compare_to == "Previous Month"
      #final['from_date'] = Date.parse(period).beginning_of_month.strftime("%Y-%m-%d")
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Year"
      #final['from_date'] = (Date.parse(period).ago(1.year).end_of_month+1).strftime("%Y-%m-%d")
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Quarter"
      #final['from_date'] = (Date.parse(period).ago(3.month).end_of_month+1).beginning_of_month.strftime("%Y-%m-%d")
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Week"
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    elsif compare_to == "Previous Day"
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    elsif compare_to == ''
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    end
    return final
  end

  # Method returns the array of dates
  # Params::
  # 1.compare_to specifies what kind of period separation
  # 2.compare_periods specifies how many periods
  # 3.month_year is the start month and year which is used to begin from.
  # this method is used in balance_sheet_controller, income_statement_controller etc.,
  def date_array(compare_to, compare_periods, month_year)
    month_year = month_year.to_date
    period_array = Array.new
    period_array << month_year
    if compare_to == "Previous Year"
      (1..compare_periods.sub("Period ", "").to_i).each {|i| period_array << month_year.ago(i.year)}
    elsif compare_to == "Previous Month"
      (1..compare_periods.sub("Period ", "").to_i).each {|i| period_array << month_year.ago(i.month)}
    elsif compare_to == "Previous Quarter"
      (1..(compare_periods.sub("Period ", "").to_i)).each {|i| period_array << month_year.ago((i * 3).month)}
    elsif compare_to == "Previous Week"
      (1..(compare_periods.sub("Period ", "").to_i)).each {|i| period_array << month_year.ago(i.weeks)}
    elsif compare_to == "Previous Day"
      (1..(compare_periods.sub("Period ", "").to_i)).each {|i| period_array << month_year.ago(i.days)}

    end
    return period_array
  end

  #Returns an Array of the uniq custom field names regardless of the model. by BnDayel
  def list_of_custom_fields
    CpnCustomField.pluck(:field_name).uniq.compact
  end

  #Recieves a string of the custom field name and returns an array of the uniq values. by BnDayel
  def list_of_custom_values
    field_name = Mysql2::Client.escape(params[:custom_field])
    values = CpnCustomField.includes(:cpn_custom_field_values).where(field_name: field_name).pluck(:value).uniq.compact.collect{ |e| e ? e.strip : e }.uniq
    respond_to do |format|
      format.json {render json: {custom_fields: values}}
    end
  end

  # Reset the filter parameters clear the values from session
  def reset_params
    session[:compare_to] = ""
    session[:compare_periods] = ""
    session[:custom_field_analysis] = ""
    session[:balance_date] = ""
    session[:custom_field] = ""
    session[:custom_value] = ""
    session[:from] = ""
    session[:to] = ""
  end

  # Method returns the array of dates
  # Params::
  # 1.compare_to specifies what kind of period separation
  # 2.compare_periods specifies how many periods
  # 3.date is the from_date which is used to begin from.
  # this method is used in new_customer_trend_lines_controller and invoice_trend_lines_controller
  def date_array_trend_lines(compare_to, compare_periods, date)
    period_array = Array.new
    period_array << date
    if compare_periods.present?
      if compare_to == "Previous Year"
        (1..compare_periods.split[-1].to_i).each {|i| period_array << date.ago(i.year)}
      elsif compare_to == "Previous Month"
        (1..compare_periods.split[-1].to_i).each {|i| period_array << date.ago(i.month)}
      elsif compare_to == "Previous Quarter"
        (1..(compare_periods.split[-1].to_i)).each {|i| period_array << date.ago((i * 3).month)}
      elsif compare_to == "Previous Week"
        tmp_date = date
        (1..(compare_periods.split[-1].to_i)).each do |i|
          tmp_date = tmp_date - 7.day
          period_array << tmp_date
        end
      end
    end
    return period_array
  end

  # Method returns the from_date and to_date based on the comparison period
  # this method is used in new_customer_trend_lines_controller and invoice_trend_lines_controller
  def from_to_date_trend_lines(compare_to = '', period = '')
    final = {}
    if compare_to == "Previous Month"
      final['from_date'] = (Date.parse(period).ago(1.month) + 1.day).to_date.strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    elsif compare_to == "Previous Year"
      final['from_date'] = (Date.parse(period).ago(1.year).end_of_month + 1.day).strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Quarter"
      final['from_date'] = (Date.parse(period).ago(3.month).end_of_month + 1.day).beginning_of_month.strftime("%Y-%m-%d")
      final['to_date'] = Date.parse(period).end_of_month.strftime("%Y-%m-%d")
    elsif compare_to == "Previous Week"
      final['from_date'] = (Date.parse(period) - 6.day).to_date
      final['to_date'] = Date.parse(period)
    else
      final['from_date'] = begining_of_transaction
      final['to_date'] = Date.parse(period).strftime("%Y-%m-%d")
    end
    return final
  end

  def fiscal_year_start
    date_from1 = get_settings.try(:fiscal_year_start).present? ? Date.parse(get_settings.fiscal_year_start) : Date.parse('Jan')
    if Date.current.month < date_from1.strftime("%m").to_i
      tmp_date_from = date_from1 - 1.year
    else
      tmp_date_from = date_from1
    end
  end

  def get_last_fiscal_year(date_to = Date.current)
    date_to = Date.parse(date_to)
    fiscal = get_settings.try(:fiscal_year_start).present? ? Date.parse(get_settings.fiscal_year_start) : Date.parse('Jan')
    year_difference = date_to.year.to_i - fiscal.year.to_i
    if year_difference != 0
      fiscal = fiscal + year_difference.year
    end
    if date_to.month.to_i < fiscal.strftime("%m").to_i
      fiscal = fiscal - 1.year
    end
    fiscal
  end

  # Method returns the from_date and to_date based on the comparison period
  def set_from_and_to_date_for_aged_open_reports(compare_to, compare_periods, period = '')
    if compare_periods.present?
      if compare_to == "Previous Month"
        from_date = check_previous_month(period)
        to_date = period
      elsif compare_to == "Previous Year"
        from_date = (period.ago(1.year) + 1.day).to_date
        to_date = period
      elsif compare_to == "Previous Quarter"
        from_date = (period.ago(3.month) + 1.day).to_date
        to_date = period
      elsif compare_to == "Previous Week"
        from_date = (period - 6.day).to_date
        to_date = period
      elsif !compare_to.present?
        from_date = begining_of_transaction ||  Time.current.beginning_of_month.to_date
        to_date = period.presence || Time.current.to_date
      end
    else #if !@compare_to.present? || !@compare_periods.present?
      from_date = begining_of_transaction ||  Time.current.beginning_of_month.to_date
      to_date = period.presence || Time.current.to_date
    end
    return from_date, to_date
  end

  # If end of month then check by beggining of month else just subtract the dates
  def check_previous_month(period)
    if period.to_date == period.to_date.end_of_month
      period.to_date.beginning_of_month
    else
      (period.ago(1.month) + 1.day).to_date
    end
  end

  def set_date_array(compare_to, compare_periods, date)
    period_array = Array.new
    period_array << date
    if compare_periods.present?
      if compare_to == "Previous Year"
        (1..compare_periods.sub("Period ", "").to_i).each {|i| period_array << date.ago(i.year)}
      elsif compare_to == "Previous Month"
        if date == date.end_of_month
          (1..compare_periods.sub("Period ", "").to_i).each { |i| period_array << date.ago(i.month).end_of_month }
        else
          (1..compare_periods.sub("Period ", "").to_i).each {|i| period_array << date.ago(i.month)}
        end
      elsif compare_to == "Previous Quarter"
        (1..(compare_periods.sub("Period ", "").to_i)).each {|i| period_array << date.ago((i * 3).month)}
      elsif compare_to == "Previous Week"
        tmp_date = date
        (1..(compare_periods.sub("Period ", "").to_i)).each do |i|
          tmp_date = tmp_date - 7.day
          period_array << tmp_date
        end
      end
    end
    return period_array
  end

  def begining_of_transaction
    first_date = Plutus::Entry.order(date: :asc).first.try(:date).try(:to_date)
    first_date

  end

  def report_dashboard

  end

  helper_method :fiscal_year_start, :begining_of_transaction, :set_from_and_to_date_for_aged_open_reports
end
