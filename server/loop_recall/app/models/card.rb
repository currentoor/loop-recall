# == Schema Information
#
# Table name: cards
#
#  id         :integer          not null, primary key
#  question   :text
#  answer     :text
#  created_at :datetime         not null
#  updated_at :datetime         not null
#

class Card < ActiveRecord::Base
end
